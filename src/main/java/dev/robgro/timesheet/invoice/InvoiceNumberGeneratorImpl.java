package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.NoSchemeConfiguredException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Implementation of configurable invoice number generation using atomic counters.
 *
 * <p>Key features:
 * - Per-seller (tenant) numbering schemes
 * - Atomic counter-based sequence generation (no race conditions)
 * - Component-based numbering (sequence, year, month)
 * - Support for backdated invoices (uses issue date, not current date)
 * - No retry logic needed (counters handle concurrency)
 *
 * <p>Replaced MAX+1 approach with atomic UPSERT counters for thread-safety.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberGeneratorImpl implements InvoiceNumberGenerator {

    private final InvoiceNumberingSchemeRepository schemeRepository;
    private final InvoiceNumberCounterService counterService;
    private final PeriodKeyFactory periodKeyFactory;
    private final TemplateParser templateParser;

    @Override
    @Transactional
    public GeneratedInvoiceNumber generateInvoiceNumber(LocalDate issueDate, Department department) {
        if (issueDate == null) {
            throw new ValidationException("Issue date is required");
        }

        // 1. Get seller from SecurityContext (no DB query)
        Long sellerId = SecurityUtils.getCurrentSellerId();
        if (sellerId == null) {
            throw new BusinessRuleViolationException("Current user has no seller assigned");
        }

        // 2. Load scheme effective on issueDate
        InvoiceNumberingScheme scheme = schemeRepository
            .findEffectiveScheme(sellerId, issueDate)
            .orElseThrow(() -> new NoSchemeConfiguredException(
                "No numbering scheme configured for seller " + sellerId + " effective on " + issueDate
            ));

        // 3. Determine period (year, month based on reset strategy)
        PeriodComponents period = determinePeriod(issueDate, scheme.getResetPeriod());

        // 4. Build period key (must match V27 backfill)
        String periodKey = periodKeyFactory.build(scheme.getResetPeriod(), period.year(), period.month());

        // 5. Get next sequence (atomic counter with self-healing drift correction)
        int seq = counterService.nextSequence(sellerId, scheme.getResetPeriod(), periodKey, null, period.year(), period.month());

        // 6. Apply template
        // Display uses actual issueDate (year/month), NOT the counter period.
        // period.year()/month() are counter key sentinels (e.g., month=0 for YEARLY)
        // and must NOT leak into template rendering — {MM} must show actual invoice month.
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(seq)
            .year(issueDate.getYear())
            .month(issueDate.getMonthValue())
            .department(department) // Can be null - TemplateParser must handle it
            .build();

        String displayNumber = templateParser.apply(scheme.getTemplate(), context);

        log.info("Generated invoice number: {} (sellerId={}, resetPeriod={}, periodKey={}, seq={}, year={}, month={})",
            displayNumber, sellerId, scheme.getResetPeriod(), periodKey, seq, period.year(), period.month());

        return new GeneratedInvoiceNumber(seq, period.year(), period.month(), displayNumber, scheme.getId());
    }

    @Override
    public GeneratedInvoiceNumber peekNextInvoiceNumber(LocalDate issueDate, Department department) {
        if (issueDate == null) {
            throw new ValidationException("Issue date is required");
        }

        // 1. Get seller from SecurityContext (no DB query)
        Long sellerId = SecurityUtils.getCurrentSellerId();
        if (sellerId == null) {
            throw new BusinessRuleViolationException("Current user has no seller assigned");
        }

        // 2. Load scheme effective on issueDate
        InvoiceNumberingScheme scheme = schemeRepository
            .findEffectiveScheme(sellerId, issueDate)
            .orElseThrow(() -> new NoSchemeConfiguredException(
                "No numbering scheme configured for seller " + sellerId + " effective on " + issueDate
            ));

        // 3. Determine period (year, month based on reset strategy)
        PeriodComponents period = determinePeriod(issueDate, scheme.getResetPeriod());

        // 4. Build period key (must match V27 backfill)
        String periodKey = periodKeyFactory.build(scheme.getResetPeriod(), period.year(), period.month());

        // 5. PEEK next sequence (read-only, does NOT increment counter)
        int seq = counterService.peekNextSequence(sellerId, scheme.getResetPeriod(), periodKey);

        // 6. Apply template — display uses actual issueDate, not counter period sentinels
        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(seq)
            .year(issueDate.getYear())
            .month(issueDate.getMonthValue())
            .department(department)
            .build();

        String displayNumber = templateParser.apply(scheme.getTemplate(), context);

        log.info("Peeked invoice number (preview, NOT reserved): {} (sellerId={}, resetPeriod={}, periodKey={}, seq={}, year={}, month={})",
            displayNumber, sellerId, scheme.getResetPeriod(), periodKey, seq, period.year(), period.month());

        return new GeneratedInvoiceNumber(seq, period.year(), period.month(), displayNumber, scheme.getId());
    }

    /**
     * Determine period components (year, month) based on issue date and reset period.
     *
     * <p>CRITICAL: Returns consistent values for counter key generation:
     * - MONTHLY: (issueDate.year, issueDate.month)
     * - YEARLY: (issueDate.year, 0)
     * - NEVER: (0, 0) - NOT (issueDate.year, 0)
     *
     * <p>Guard: Throws if MONTHLY with month=0 (invalid state that breaks counters).
     *
     * @param issueDate Date when invoice is issued
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @return Period components with year and month
     * @throws IllegalStateException if MONTHLY reset with month=0
     */
    private PeriodComponents determinePeriod(LocalDate issueDate, ResetPeriod resetPeriod) {
        PeriodComponents period = switch (resetPeriod) {
            case MONTHLY -> new PeriodComponents(issueDate.getYear(), issueDate.getMonthValue());
            case YEARLY  -> new PeriodComponents(issueDate.getYear(), 0);
            case NEVER   -> new PeriodComponents(0, 0); // CRITICAL: was (year, 0) - fixed!
        };

        // Guard: prevent MONTHLY + month=0 (would break counter consistency)
        if (resetPeriod == ResetPeriod.MONTHLY && period.month() == 0) {
            throw new IllegalStateException(
                "Invalid period: MONTHLY reset cannot have month=0. issueDate=" + issueDate
            );
        }

        return period;
    }
}
