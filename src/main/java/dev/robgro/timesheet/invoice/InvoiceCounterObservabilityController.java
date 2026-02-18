package dev.robgro.timesheet.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Internal observability endpoint for invoice counter monitoring and drift detection.
 *
 * <p><b>ADMIN ONLY:</b> This endpoint is restricted to users with ADMIN role.
 * Used by support/ops to verify counter integrity and debug numbering issues.
 *
 * <p><b>Use cases:</b>
 * - Support ticket: "Why did invoice number jump from 003 to 017?"
 * - Post-migration verification: Check if V27 backfill worked correctly
 * - Drift detection: Alert when counter.lastValue != MAX(sequence_number)
 * - Multi-tenant audit: Quick health check without direct DB access
 *
 * <p><b>Feature flag:</b> Can be disabled via property:
 * {@code internal.counters.observability.enabled=false}
 *
 * <p><b>Security:</b>
 * - Audit logged (username + sellerId)
 * - ADMIN role required
 * - Multi-tenant isolation enforced
 */
@RestController
@RequestMapping("/internal/invoice-counters")
@PreAuthorize("hasRole('ADMIN')")
@ConditionalOnProperty(
    name = "internal.counters.observability.enabled",
    havingValue = "true",
    matchIfMissing = true
)
@RequiredArgsConstructor
@Slf4j
public class InvoiceCounterObservabilityController {

    private final InvoiceNumberCounterRepository counterRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberingSchemeRepository schemeRepository;

    /**
     * Get counter status with drift detection for specified seller.
     *
     * <p>Returns all counters for the seller, including:
     * - Current counter value
     * - Actual invoice count
     * - Drift detection (counter != reality)
     *
     * @param sellerId Tenant ID to check counters for
     * @return Counter observability response with drift detection
     */
    @GetMapping
    public CounterObservabilityResponse getCounters(@RequestParam Long sellerId) {
        // Audit log - track who accessed which seller's data
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "anonymous";
        log.info("📊 Counter observability accessed: user={}, sellerId={}", username, sellerId);

        // Get all counters for seller (ordered by reset period, newest first)
        List<InvoiceNumberCounter> counters = counterRepository
            .findBySellerIdOrderByResetPeriodAscPeriodKeyDesc(sellerId);

        // Build status for each counter with drift detection
        List<CounterStatusDto> counterStatus = counters.stream()
            .map(counter -> buildCounterStatus(sellerId, counter))
            .toList();

        // Get current active template (or null if no scheme configured)
        String currentTemplate = schemeRepository
            .findEffectiveScheme(sellerId, LocalDate.now())
            .map(InvoiceNumberingScheme::getTemplate)
            .orElse(null);

        return new CounterObservabilityResponse(sellerId, currentTemplate, counterStatus);
    }

    /**
     * Build counter status with drift detection for a single counter.
     *
     * <p>Drift detection logic:
     * <ol>
     *   <li>Get counter's last value (what counter thinks is the last sequence)</li>
     *   <li>Get MAX(sequence_number) from invoices table (actual reality)</li>
     *   <li>Compare: if counter != MAX(sequence), drift detected</li>
     * </ol>
     *
     * <p>Drift can occur due to:
     * - Race conditions (should be impossible with atomic UPSERT)
     * - Manual database edits
     * - Migration errors (V27 backfill incorrect)
     * - Transaction rollbacks
     *
     * @param sellerId Seller ID (for querying invoices)
     * @param counter Counter entity to build status for
     * @return Counter status DTO with drift detection
     */
    private CounterStatusDto buildCounterStatus(Long sellerId, InvoiceNumberCounter counter) {
        // Extract period components from counter
        int periodYear = extractYearFromPeriodKey(counter.getPeriodKey(), counter.getResetPeriod());
        int periodMonth = extractMonthFromPeriodKey(counter.getPeriodKey(), counter.getResetPeriod());

        // Get actual invoice data from database
        Long invoiceCount = invoiceRepository.countBySellerIdAndPeriodYearAndPeriodMonth(
            sellerId, periodYear, periodMonth
        );

        Integer maxSequence = invoiceRepository.findMaxSequenceNumber(
            sellerId, periodYear, periodMonth
        );

        // Get last invoice display number (for debugging)
        String lastInvoiceNumber = invoiceRepository
            .findTopBySellerIdAndPeriodYearAndPeriodMonthOrderBySequenceNumberDesc(
                sellerId, periodYear, periodMonth
            )
            .map(Invoice::getInvoiceNumberDisplay)
            .orElse(null);

        // Drift detection: counter.lastValue should equal MAX(sequence_number)
        Integer expectedValue = maxSequence != null ? maxSequence : 0;
        boolean hasDrift = !counter.getLastValue().equals(expectedValue);

        // Log drift detection for monitoring
        if (hasDrift) {
            log.warn("⚠️ Counter drift detected: sellerId={}, periodKey={}, counter={}, actual={}",
                sellerId, counter.getPeriodKey(), counter.getLastValue(), expectedValue);
        }

        return new CounterStatusDto(
            counter.getResetPeriod(),
            counter.getPeriodKey(),
            counter.getLastValue(),
            counter.getUpdatedAt(),
            invoiceCount,
            lastInvoiceNumber,
            hasDrift,
            expectedValue
        );
    }

    /**
     * Extract year from period key based on reset period.
     *
     * <p>Examples:
     * - MONTHLY "2026-02" → 2026
     * - YEARLY "2026" → 2026
     * - NEVER "NEVER" → 0
     *
     * @param periodKey Period key string
     * @param resetPeriod Reset strategy
     * @return Year value (0 for NEVER)
     */
    private int extractYearFromPeriodKey(String periodKey, ResetPeriod resetPeriod) {
        return switch (resetPeriod) {
            case MONTHLY -> Integer.parseInt(periodKey.substring(0, 4)); // "2026-02" → 2026
            case YEARLY -> Integer.parseInt(periodKey); // "2026" → 2026
            case NEVER -> 0; // "NEVER" → 0
        };
    }

    /**
     * Extract month from period key based on reset period.
     *
     * <p>Examples:
     * - MONTHLY "2026-02" → 2
     * - YEARLY "2026" → 0
     * - NEVER "NEVER" → 0
     *
     * @param periodKey Period key string
     * @param resetPeriod Reset strategy
     * @return Month value (0 for YEARLY/NEVER)
     */
    private int extractMonthFromPeriodKey(String periodKey, ResetPeriod resetPeriod) {
        return switch (resetPeriod) {
            case MONTHLY -> Integer.parseInt(periodKey.substring(5, 7)); // "2026-02" → 2
            case YEARLY, NEVER -> 0; // No month component
        };
    }
}
