package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

        InvoiceNumberCounter staleCounter = new InvoiceNumberCounter();
        staleCounter.setLastValue(1);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.of(staleCounter));

        // Simulate: after heal, bump sets LAST_INSERT_ID to 11
        when(repository.lastInsertId()).thenReturn(11L);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 2);

        // Then - healed to 10, then bumped to 11
        assertThat(result).isEqualTo(11);
        verify(repository).healCounterIfBehind(sellerId, "MONTHLY", periodKey, null, 10);
        verify(repository).bumpAndSetLastInsertId(sellerId, "MONTHLY", periodKey, null);
    }

    @Test
    void shouldNotHeal_whenCounterMatchesMaxSeq() {
        // Given - counter is up to date
        Long sellerId = 1L;
        String periodKey = "2026-02";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2026, 2)).thenReturn(5);

        InvoiceNumberCounter upToDateCounter = new InvoiceNumberCounter();
        upToDateCounter.setLastValue(5);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.of(upToDateCounter));

        when(repository.lastInsertId()).thenReturn(6L);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 2);

        // Then - no healing needed
        assertThat(result).isEqualTo(6);
        verify(repository, never()).healCounterIfBehind(any(), any(), any(), any(), anyInt());
        verify(repository).bumpAndSetLastInsertId(sellerId, "MONTHLY", periodKey, null);
    }

    @Test
    void shouldNotHeal_whenNoInvoicesExist() {
        // Given - first invoice in this period, no MAX(seq)
        Long sellerId = 1L;
        String periodKey = "2026-03";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2026, 3)).thenReturn(null);
        when(repository.lastInsertId()).thenReturn(1L);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2026, 3);

        // Then - nothing to heal, normal bump
        assertThat(result).isEqualTo(1);
        verify(repository, never()).healCounterIfBehind(any(), any(), any(), any(), anyInt());
        verify(repository, never()).findBySellerIdAndResetPeriodAndPeriodKey(any(), any(), any());
    }

    @Test
    void shouldHeal_whenCounterMissingButInvoicesExist() {
        // Given - counter lost (e.g., manual delete), but invoices with seq=34 exist
        Long sellerId = 1L;
        String periodKey = "2021-01";

        when(invoiceRepository.findMaxSequenceNumber(sellerId, 2021, 1)).thenReturn(34);
        when(repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, ResetPeriod.MONTHLY, periodKey))
            .thenReturn(Optional.empty()); // counter missing

        when(repository.lastInsertId()).thenReturn(35L);

        // When
        int result = service.nextSequence(sellerId, ResetPeriod.MONTHLY, periodKey, null, 2021, 1);

        // Then - healed to 34 (via INSERT), then bumped to 35
        assertThat(result).isEqualTo(35);
        verify(repository).healCounterIfBehind(sellerId, "MONTHLY", periodKey, null, 34);
    }
}