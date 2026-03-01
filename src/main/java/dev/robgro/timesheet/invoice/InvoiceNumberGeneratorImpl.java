package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.NoSchemeConfiguredException;
import dev.robgro.timesheet.exception.ValidationException;
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
    public GeneratedInvoiceNumber generateInvoiceNumber(Long sellerId, LocalDate issueDate, Department department) {
        if (sellerId == null) {
            throw new BusinessRuleViolationException("sellerId is required for invoice number generation");
        }
        if (issueDate == null) {
            throw new ValidationException("Issue date is required");
        }

        InvoiceNumberingScheme scheme = schemeRepository
            .findEffectiveScheme(sellerId, issueDate)
            .orElseThrow(() -> new NoSchemeConfiguredException(
                "No numbering scheme configured for seller " + sellerId + " effective on " + issueDate
            ));

        PeriodComponents period = determinePeriod(issueDate, scheme.getResetPeriod());
        String periodKey = periodKeyFactory.build(scheme.getResetPeriod(), period.year(), period.month());
        int seq = counterService.nextSequence(sellerId, scheme.getResetPeriod(), periodKey, null, period.year(), period.month());

        TemplateContext context = TemplateContext.builder()
            .sequenceNumber(seq)
            .year(issueDate.getYear())
            .month(issueDate.getMonthValue())
            .department(department)
            .build();

        String displayNumber = templateParser.apply(scheme.getTemplate(), context);

        log.info("Generated invoice number: {} (sellerId={}, resetPeriod={}, periodKey={}, seq={}, year={}, month={})",
            displayNumber, sellerId, scheme.getResetPeriod(), periodKey, seq, period.year(), period.month());

        return new GeneratedInvoiceNumber(seq, period.year(), period.month(), displayNumber, scheme.getId());
    }

    @Override
    public GeneratedInvoiceNumber peekNextInvoiceNumber(Long sellerId, LocalDate issueDate, Department department) {
        if (sellerId == null) {
            throw new BusinessRuleViolationException("sellerId is required for invoice number generation");
        }
        if (issueDate == null) {
            throw new ValidationException("Issue date is required");
        }

        InvoiceNumberingScheme scheme = schemeRepository
            .findEffectiveScheme(sellerId, issueDate)
            .orElseThrow(() -> new NoSchemeConfiguredException(
                "No numbering scheme configured for seller " + sellerId + " effective on " + issueDate
            ));

        PeriodComponents period = determinePeriod(issueDate, scheme.getResetPeriod());
        String periodKey = periodKeyFactory.build(scheme.getResetPeriod(), period.year(), period.month());
        int seq = counterService.peekNextSequence(sellerId, scheme.getResetPeriod(), periodKey);

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

    private PeriodComponents determinePeriod(LocalDate issueDate, ResetPeriod resetPeriod) {
        PeriodComponents period = switch (resetPeriod) {
            case MONTHLY -> new PeriodComponents(issueDate.getYear(), issueDate.getMonthValue());
            case YEARLY  -> new PeriodComponents(issueDate.getYear(), 0);
            case NEVER   -> new PeriodComponents(0, 0);
        };

        if (resetPeriod == ResetPeriod.MONTHLY && period.month() == 0) {
            throw new IllegalStateException(
                "Invalid period: MONTHLY reset cannot have month=0. issueDate=" + issueDate
            );
        }

        return period;
    }
}