package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.NoSchemeConfiguredException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.seller.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceNumberGeneratorImplTest {

    @Mock
    private InvoiceNumberingSchemeRepository schemeRepository;

    @Mock
    private InvoiceNumberCounterService counterService;

    @Mock
    private PeriodKeyFactory periodKeyFactory;

    @Mock
    private TemplateParser templateParser;

    @InjectMocks
    private InvoiceNumberGeneratorImpl generator;

    private Seller testSeller;
    private InvoiceNumberingScheme testScheme;

    @BeforeEach
    void setUp() {
        // Create test seller
        testSeller = new Seller();
        testSeller.setId(1L);
        testSeller.setName("Test Company");
        testSeller.setActive(true);

        // Create test scheme
        testScheme = InvoiceNumberingScheme.create(
            testSeller,
            "{SEQ:3}-{MM}-{YYYY}",
            ResetPeriod.MONTHLY,
            LocalDate.of(2020, 1, 1),
            1
        );
    }

    @Test
    void shouldGenerateInvoiceNumberForMonthlyReset() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(testScheme));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2026, 2)).thenReturn("2026-02");
        when(counterService.nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2)).thenReturn(1);
        when(templateParser.apply(eq("{SEQ:3}-{MM}-{YYYY}"), any()))
            .thenReturn("001-02-2026");

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getPeriodYear()).isEqualTo(2026);
        assertThat(result.getPeriodMonth()).isEqualTo(2);
        assertThat(result.getDisplayNumber()).isEqualTo("001-02-2026");

        verify(counterService).nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2);
    }

    @Test
    void shouldIncrementSequenceNumber() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(testScheme));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2026, 2)).thenReturn("2026-02");
        when(counterService.nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2)).thenReturn(6); // Counter returns 6
        when(templateParser.apply(eq("{SEQ:3}-{MM}-{YYYY}"), any()))
            .thenReturn("006-02-2026");

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(6);
    }

    @Test
    void shouldGenerateNumberForYearlyReset() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);
        InvoiceNumberingScheme yearlyScheme = InvoiceNumberingScheme.create(
            testSeller,
            "INV-{SEQ:3}-{YYYY}",
            ResetPeriod.YEARLY,
            LocalDate.of(2020, 1, 1),
            1
        );

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(yearlyScheme));
        when(periodKeyFactory.build(ResetPeriod.YEARLY, 2026, 0)).thenReturn("2026");
        when(counterService.nextSequence(1L, ResetPeriod.YEARLY, "2026", null, 2026, 0)).thenReturn(1);
        when(templateParser.apply(eq("INV-{SEQ:3}-{YYYY}"), any()))
            .thenReturn("INV-001-2026");

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getPeriodYear()).isEqualTo(2026);
        assertThat(result.getPeriodMonth()).isEqualTo(0); // YEARLY - month is 0
    }

    @Test
    void shouldGenerateNumberForNeverReset() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);
        InvoiceNumberingScheme neverScheme = InvoiceNumberingScheme.create(
            testSeller,
            "INV-{SEQ:4}",
            ResetPeriod.NEVER,
            LocalDate.of(2020, 1, 1),
            1
        );

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(neverScheme));
        when(periodKeyFactory.build(ResetPeriod.NEVER, 0, 0)).thenReturn("NEVER");
        when(counterService.nextSequence(1L, ResetPeriod.NEVER, "NEVER", null, 0, 0)).thenReturn(42);
        when(templateParser.apply(eq("INV-{SEQ:4}"), any()))
            .thenReturn("INV-0042");

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(42);
        assertThat(result.getPeriodYear()).isEqualTo(0); // NEVER - year is 0
        assertThat(result.getPeriodMonth()).isEqualTo(0); // NEVER - month is 0
        assertThat(result.getDisplayNumber()).isEqualTo("INV-0042");
    }

    @Test
    void shouldThrowExceptionWhenNoSchemeConfigured() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> generator.generateInvoiceNumber(1L, issueDate, null))
            .isInstanceOf(NoSchemeConfiguredException.class)
            .hasMessageContaining("No numbering scheme configured");
    }

    @Test
    void shouldSupportBackdatedInvoices() {
        // Given - invoice issued in the past
        LocalDate backdatedIssueDate = LocalDate.of(2025, 12, 15);

        when(schemeRepository.findEffectiveScheme(1L, backdatedIssueDate)).thenReturn(Optional.of(testScheme));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2025, 12)).thenReturn("2025-12");
        when(counterService.nextSequence(1L, ResetPeriod.MONTHLY, "2025-12", null, 2025, 12)).thenReturn(11);
        when(templateParser.apply(any(), any())).thenReturn("011-12-2025");

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, backdatedIssueDate, null);

        // Then
        assertThat(result.getSequenceNumber()).isEqualTo(11);
        assertThat(result.getPeriodYear()).isEqualTo(2025);
        assertThat(result.getPeriodMonth()).isEqualTo(12);

        // Verify it used the backdated issue date, not current date
        verify(schemeRepository).findEffectiveScheme(1L, backdatedIssueDate);
        verify(periodKeyFactory).build(ResetPeriod.MONTHLY, 2025, 12);
    }

    @Test
    void shouldResetSequenceWhenMonthChanges() {
        // Given - February, new month
        LocalDate februaryDate = LocalDate.of(2026, 2, 15);

        when(schemeRepository.findEffectiveScheme(any(), any())).thenReturn(Optional.of(testScheme));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2026, 2)).thenReturn("2026-02");
        when(counterService.nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2)).thenReturn(1); // New month, starts at 1
        when(templateParser.apply(any(), any())).thenReturn("001-02-2026");

        // When - generate for February
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, februaryDate, null);

        // Then - should be sequence 1 (new month)
        assertThat(result.getSequenceNumber()).isEqualTo(1);
        assertThat(result.getPeriodMonth()).isEqualTo(2);
    }

    @Test
    void shouldThrowExceptionWhenIssueDateIsNull() {
        // When & Then
        assertThatThrownBy(() -> generator.generateInvoiceNumber(1L, null, null))
            .isInstanceOf(ValidationException.class)
            .hasMessageContaining("Issue date is required");
    }

    @Test
    void shouldThrowExceptionWhenSellerIdIsNull() {
        // When & Then
        assertThatThrownBy(() -> generator.generateInvoiceNumber(null, LocalDate.now(), null))
            .isInstanceOf(BusinessRuleViolationException.class)
            .hasMessageContaining("sellerId is required");
    }

    // ===== schemeId audit trail tests =====

    @Test
    void shouldReturnSameSchemeId_fromBothPeekAndGenerate() {
        // Given
        LocalDate issueDate = LocalDate.of(2026, 2, 15);
        InvoiceNumberingScheme schemeA = mock(InvoiceNumberingScheme.class);
        when(schemeA.getId()).thenReturn(42L);
        when(schemeA.getTemplate()).thenReturn("{SEQ:3}-{MM}-{YYYY}");
        when(schemeA.getResetPeriod()).thenReturn(ResetPeriod.MONTHLY);

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(schemeA));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2026, 2)).thenReturn("2026-02");
        when(counterService.nextSequence(1L, ResetPeriod.MONTHLY, "2026-02", null, 2026, 2)).thenReturn(1);
        when(counterService.peekNextSequence(1L, ResetPeriod.MONTHLY, "2026-02")).thenReturn(1);
        when(templateParser.apply(any(), any())).thenReturn("001-02-2026");

        // When
        GeneratedInvoiceNumber generated = generator.generateInvoiceNumber(1L, issueDate, null);
        GeneratedInvoiceNumber peeked = generator.peekNextInvoiceNumber(1L, issueDate, null);

        // Then
        assertThat(generated.getSchemeId()).isEqualTo(42L);
        assertThat(peeked.getSchemeId()).isEqualTo(42L);
        assertThat(generated.getSchemeId()).isEqualTo(peeked.getSchemeId());
    }

    @Test
    void shouldCaptureDifferentSchemeId_whenSchemeChanges() {
        // Given - simulates scheme switch (archive A, activate B) between two invoice creations
        LocalDate issueDate = LocalDate.of(2026, 2, 15);

        InvoiceNumberingScheme schemeA = mock(InvoiceNumberingScheme.class);
        when(schemeA.getId()).thenReturn(100L);
        when(schemeA.getTemplate()).thenReturn("{SEQ:3}-{MM}-{YYYY}");
        when(schemeA.getResetPeriod()).thenReturn(ResetPeriod.MONTHLY);

        InvoiceNumberingScheme schemeB = mock(InvoiceNumberingScheme.class);
        when(schemeB.getId()).thenReturn(200L);
        when(schemeB.getTemplate()).thenReturn("INV-{SEQ:4}-{YYYY}");
        when(schemeB.getResetPeriod()).thenReturn(ResetPeriod.YEARLY);

        when(schemeRepository.findEffectiveScheme(eq(1L), any()))
            .thenReturn(Optional.of(schemeA))
            .thenReturn(Optional.of(schemeB));
        when(periodKeyFactory.build(ResetPeriod.MONTHLY, 2026, 2)).thenReturn("2026-02");
        when(periodKeyFactory.build(ResetPeriod.YEARLY, 2026, 0)).thenReturn("2026");
        when(counterService.nextSequence(eq(1L), eq(ResetPeriod.MONTHLY), eq("2026-02"), any(), eq(2026), eq(2))).thenReturn(1);
        when(counterService.nextSequence(eq(1L), eq(ResetPeriod.YEARLY), eq("2026"), any(), eq(2026), eq(0))).thenReturn(1);
        when(templateParser.apply(any(), any())).thenReturn("001-02-2026", "INV-0001-2026");

        // When
        GeneratedInvoiceNumber first = generator.generateInvoiceNumber(1L, issueDate, null);
        GeneratedInvoiceNumber second = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then - each invoice records the scheme that was active at generation time
        assertThat(first.getSchemeId()).isEqualTo(100L);
        assertThat(second.getSchemeId()).isEqualTo(200L);
        assertThat(first.getSchemeId()).isNotEqualTo(second.getSchemeId());
    }

    @Test
    void shouldProduceDisplayNumberConsistentWithSchemeAndIssueDate() {
        // Critical contract: for YEARLY, period.month()=0 is a counter sentinel only.
        // Display must show the ACTUAL issue month, never the sentinel.
        LocalDate issueDate = LocalDate.of(2026, 3, 15);
        String template = "INV-{SEQ:3}-{MM}-{YYYY}";

        InvoiceNumberingScheme scheme = mock(InvoiceNumberingScheme.class);
        when(scheme.getId()).thenReturn(77L);
        when(scheme.getTemplate()).thenReturn(template);
        when(scheme.getResetPeriod()).thenReturn(ResetPeriod.YEARLY);

        when(schemeRepository.findEffectiveScheme(1L, issueDate)).thenReturn(Optional.of(scheme));
        when(periodKeyFactory.build(ResetPeriod.YEARLY, 2026, 0)).thenReturn("2026");
        when(counterService.nextSequence(1L, ResetPeriod.YEARLY, "2026", null, 2026, 0)).thenReturn(5);

        // Use real TemplateParser to compute expected display — month=3 (actual), NOT 0 (sentinel)
        TemplateParser realParser = new TemplateParser();
        String expectedDisplay = realParser.apply(template, TemplateContext.builder()
            .sequenceNumber(5)
            .year(2026)
            .month(3)
            .build());
        when(templateParser.apply(eq(template), any())).thenReturn(expectedDisplay);

        // When
        GeneratedInvoiceNumber result = generator.generateInvoiceNumber(1L, issueDate, null);

        // Then: period sentinel stored as 0 (counter key), schemeId set
        assertThat(result.getPeriodMonth()).isEqualTo(0);
        assertThat(result.getSchemeId()).isEqualTo(77L);

        // Re-render using result's components + actual issueDate → must match displayNumber
        String rerendered = realParser.apply(template, TemplateContext.builder()
            .sequenceNumber(result.getSequenceNumber())
            .year(issueDate.getYear())
            .month(issueDate.getMonthValue())  // actual month, NOT period sentinel
            .build());
        assertThat(result.getDisplayNumber()).isEqualTo(rerendered);
        assertThat(result.getDisplayNumber()).contains("03");  // actual March in display
    }
}
