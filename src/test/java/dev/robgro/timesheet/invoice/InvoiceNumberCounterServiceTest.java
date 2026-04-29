package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberCounterServiceTest {

    @Mock
    private InvoiceNumberCounterRepository repository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private InvoiceNumberCounterService service;

    // ===== Self-healing tests =====

    @Test
    void shouldHealCounterAndReturnCorrectSequence_whenCounterBehindMaxSeq() {
        // Given - invoice with seq=10 exists but counter is stuck at 1 (drift)
        Long sellerId = 1L;
        String periodKey = "2026-02";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2026, 2)).thenReturn(10);
        when(invoiceRepository.countBySellerIdAndPeriodYearAndPeriodMonth(sellerId, 2026, 2)).thenReturn(3L);

        InvoiceNumberCounter staleCounter = new InvoiceNumberCounter();
        staleCounter.setLastValue(1);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.of(staleCounter));

        // After bump, last_value = 11
        when(repository.findLastValue(sellerId, "MONTHLY", periodKey)).thenReturn(11);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 2);

        // Then - healed to 10, then bumped to 11
        assertThat(result).isEqualTo(11);
        verify(repository).healCounterIfBehind(sellerId, "MONTHLY", periodKey, null, 10);
        verify(repository).bumpCounter(sellerId, "MONTHLY", periodKey, null);
        verify(repository).findLastValue(sellerId, "MONTHLY", periodKey);
    }

    @Test
    void shouldNotHeal_whenCounterMatchesMaxSeq() {
        // Given - counter is up to date
        Long sellerId = 1L;
        String periodKey = "2026-02";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2026, 2)).thenReturn(5);
        when(invoiceRepository.countBySellerIdAndPeriodYearAndPeriodMonth(sellerId, 2026, 2)).thenReturn(2L);

        InvoiceNumberCounter upToDateCounter = new InvoiceNumberCounter();
        upToDateCounter.setLastValue(5);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.of(upToDateCounter));

        when(repository.findLastValue(sellerId, "MONTHLY", periodKey)).thenReturn(6);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 2);

        // Then - no healing needed
        assertThat(result).isEqualTo(6);
        verify(repository, never()).healCounterIfBehind(any(), any(), any(), any(), anyInt());
        verify(repository).bumpCounter(sellerId, "MONTHLY", periodKey, null);
    }

    @Test
    void shouldNotHeal_whenNoInvoicesExist() {
        // Given - first invoice in this period, no MAX(seq)
        Long sellerId = 1L;
        String periodKey = "2026-03";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2026, 3)).thenReturn(null);
        when(repository.findLastValue(sellerId, "MONTHLY", periodKey)).thenReturn(1);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 3);

        // Then - nothing to heal, normal bump
        assertThat(result).isEqualTo(1);
        verify(repository, never()).healCounterIfBehind(any(), any(), any(), any(), anyInt());
        verify(repository, never()).findBySellerIdAndResetPeriodAndPeriodKey(any(), any(), any());
        verify(repository).bumpCounter(sellerId, "MONTHLY", periodKey, null);
    }

    @Test
    void shouldHeal_whenCounterMissingButInvoicesExist() {
        // Given - counter lost (e.g., manual delete), but invoices with seq=34 exist
        Long sellerId = 1L;
        String periodKey = "2021-01";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2021, 1)).thenReturn(34);
        when(invoiceRepository.countBySellerIdAndPeriodYearAndPeriodMonth(sellerId, 2021, 1)).thenReturn(5L);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.empty()); // counter missing

        when(repository.findLastValue(sellerId, "MONTHLY", periodKey)).thenReturn(35);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2021, 1);

        // Then - healed to 34 (via INSERT), then bumped to 35
        assertThat(result).isEqualTo(35);
        verify(repository).healCounterIfBehind(sellerId, "MONTHLY", periodKey, null, 34);
    }

    // ===== Period consistency guard tests =====

    @Test
    void shouldThrow_whenPeriodKeyMismatchesPeriodMonth() {
        // periodKey says "02" but periodMonth=3 — mismatch must be caught
        assertThatThrownBy(() ->
            service.nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 3))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Period mismatch");
    }

    @Test
    void shouldThrow_whenPeriodKeyMismatchesPeriodYear() {
        assertThatThrownBy(() ->
            service.nextSequence(1L, ResetPeriod.MONTHLY, "2025-03", null, 2026, 3))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Period mismatch");
    }

    @Test
    void shouldNotThrow_whenPeriodKeyMatchesPeriodYearAndMonth() {
        // Ensure consistent params don't throw; short-circuit test (no DB calls for fresh period)
        when(invoiceRepository.findMaxSequenceNumber(1L, 2026, 3)).thenReturn(null);
        when(repository.findLastValue(1L, "MONTHLY", "2026-03")).thenReturn(1);

        assertThat(service.nextSequence(1L, ResetPeriod.MONTHLY, "2026-03", null, 2026, 3))
            .isEqualTo(1);
    }

    // ===== peekNextSequence tests =====

    @Test
    void peekNextSequence_shouldReturn1_whenNoCounterExists() {
        when(repository.findLastValue(1L, "MONTHLY", "2026-03")).thenReturn(null);
        assertThat(service.peekNextSequence(1L, ResetPeriod.MONTHLY, "2026-03")).isEqualTo(1);
    }

    @Test
    void peekNextSequence_shouldReturnLastValuePlusOne_whenCounterExists() {
        when(repository.findLastValue(1L, "MONTHLY", "2026-02")).thenReturn(9);
        assertThat(service.peekNextSequence(1L, ResetPeriod.MONTHLY, "2026-02")).isEqualTo(10);
    }

    // ===== counterExistsForPeriod tests =====

    @Test
    void counterExistsForPeriod_shouldReturnFalse_whenNoCounter() {
        when(repository.findLastValue(1L, "MONTHLY", "2099-01")).thenReturn(null);
        assertThat(service.counterExistsForPeriod(1L, ResetPeriod.MONTHLY, "2099-01")).isFalse();
    }

    @Test
    void counterExistsForPeriod_shouldReturnTrue_whenCounterExists() {
        when(repository.findLastValue(1L, "MONTHLY", "2026-02")).thenReturn(64);
        assertThat(service.counterExistsForPeriod(1L, ResetPeriod.MONTHLY, "2026-02")).isTrue();
    }
}