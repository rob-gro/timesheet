package dev.robgro.timesheet.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for atomic invoice number sequence generation.
 *
 * <p>Uses MySQL/MariaDB UPSERT pattern to atomically increment counters without race conditions.
 * Thread-safe: Multiple concurrent requests for same scope will get unique sequence numbers.
 *
 * <p>Self-healing: Before each increment, checks MAX(sequence_number) in invoices table.
 * If counter is behind (drift detected), heals automatically using GREATEST() UPSERT.
 * This prevents duplicate number collisions caused by:
 * - V27 backfill bugs (COUNT vs MAX)
 * - Race conditions with transaction rollbacks
 * - Manual database edits
 *
 * <p>Fix 2026-03: Replaced LAST_INSERT_ID() retrieval with direct last_value read.
 * Root cause of February 2026 incident: on fresh INSERT, LAST_INSERT_ID() returned the
 * auto_increment row ID (64) instead of the sequence value (1), because MariaDB sets
 * LAST_INSERT_ID to the generated PK when no LAST_INSERT_ID() expression is used in INSERT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceNumberCounterService {

    private final InvoiceNumberCounterRepository repository;
    private final InvoiceRepository invoiceRepository;

    /**
     * Get next sequence number for given scope (atomically), with self-healing drift correction.
     *
     * <p>Self-healing logic (runs before each increment):
     * <ol>
     *   <li>Query MAX(sequence_number) from invoices for this seller+period</li>
     *   <li>Query current counter value</li>
     *   <li>If counter is behind MAX(seq) → heal via GREATEST() UPSERT</li>
     *   <li>Increment counter atomically</li>
     *   <li>Read back last_value directly from table (no LAST_INSERT_ID)</li>
     * </ol>
     *
     * @param sellerId Tenant ID (multi-tenant isolation)
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodKey Period identifier matching V27 format (e.g., "2026-02", "2026", "NEVER")
     * @param fyStartYear Fiscal year start (currently unused, pass null)
     * @param periodYear Period year for MAX(seq) check (e.g., 2026; 0 for NEVER)
     * @param periodMonth Period month for MAX(seq) check (1-12 for MONTHLY, 0 for YEARLY/NEVER)
     * @return Next sequence number (1, 2, 3, ...)
     */
    @Transactional
    public int nextSequence(Long sellerId, ResetPeriod resetPeriod, String periodKey,
                            Integer fyStartYear, int periodYear, int periodMonth) {
        log.debug("Generating next sequence: sellerId={}, resetPeriod={}, periodKey={}", sellerId, resetPeriod, periodKey);

        enforceMonthlyPeriodConsistency(resetPeriod, periodKey, periodYear, periodMonth);

        healIfDrifted(sellerId, resetPeriod, periodKey, fyStartYear, periodYear, periodMonth);

        repository.bumpCounter(sellerId, resetPeriod.name(), periodKey, fyStartYear);

        Integer seq = repository.findLastValue(sellerId, resetPeriod.name(), periodKey);
        if (seq == null) {
            throw new IllegalStateException(
                "Counter not found after bump for periodKey=" + periodKey + ", sellerId=" + sellerId);
        }

        log.debug("Generated sequence: {} for sellerId={}, periodKey={}", seq, sellerId, periodKey);
        return seq;
    }

    /**
     * Peek next sequence number WITHOUT incrementing counter.
     *
     * <p>CRITICAL: Read-only operation for preview purposes.
     * Does NOT heal drift — preview may show an optimistic next number.
     * Actual invoice generation must use {@link #nextSequence} to reserve number.
     *
     * @param sellerId Tenant ID (multi-tenant isolation)
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodKey Period identifier matching V27 format (e.g., "2026-02", "2026", "NEVER")
     * @return Next sequence number (1 if no counter exists, lastValue+1 otherwise)
     */
    public int peekNextSequence(Long sellerId, ResetPeriod resetPeriod, String periodKey) {
        log.debug("Peeking next sequence (read-only): sellerId={}, resetPeriod={}, periodKey={}",
            sellerId, resetPeriod, periodKey);

        Integer last = repository.findLastValue(sellerId, resetPeriod.name(), periodKey);
        return (last == null) ? 1 : last + 1;
    }

    /**
     * Check whether a counter row already exists for the given scope.
     *
     * <p>Used by {@code InvoiceCreationServiceImpl} for the fail-fast anomaly guard:
     * if it's the first invoice of a fresh period but a counter already exists,
     * something went wrong (e.g., stale counter from failed UPSERT).
     *
     * @param sellerId Tenant ID
     * @param resetPeriod Reset strategy
     * @param periodKey Period identifier
     * @return true if counter row exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean counterExistsForPeriod(Long sellerId, ResetPeriod resetPeriod, String periodKey) {
        return repository.findLastValue(sellerId, resetPeriod.name(), periodKey) != null;
    }

    /**
     * Validate that periodKey, periodYear, and periodMonth are mutually consistent for MONTHLY reset.
     *
     * <p>Prevents silent data corruption if caller passes mismatched parameters
     * (e.g., periodKey="2026-02" but periodMonth=3).
     */
    private void enforceMonthlyPeriodConsistency(ResetPeriod resetPeriod, String periodKey,
                                                  int periodYear, int periodMonth) {
        if (resetPeriod != ResetPeriod.MONTHLY) return;
        if (periodKey == null || periodKey.length() != 7 || periodKey.charAt(4) != '-') return;
        try {
            int y = Integer.parseInt(periodKey.substring(0, 4));
            int m = Integer.parseInt(periodKey.substring(5, 7));
            if (y != periodYear || m != periodMonth) {
                throw new IllegalStateException(
                    "Period mismatch: periodKey=" + periodKey +
                    " but periodYear=" + periodYear + " periodMonth=" + periodMonth);
            }
        } catch (NumberFormatException ignored) { }
    }

    /**
     * Check for counter drift and heal if behind MAX(sequence_number) in invoices.
     *
     * <p>Safe: uses GREATEST() so counter never goes backwards.
     * Idempotent: calling twice has same effect as calling once.
     */
    private void healIfDrifted(Long sellerId, ResetPeriod resetPeriod, String periodKey,
                               Integer fyStartYear, int periodYear, int periodMonth) {
        Integer maxSeq = invoiceRepository.findMaxSequenceNumber(sellerId, periodYear, periodMonth);
        if (maxSeq == null || maxSeq <= 0) {
            return; // No active (non-cancelled) invoices yet — nothing to heal
        }

        // Sanity check: if MAX > 0 but actual invoice count is 0, we have a phantom MAX
        // (data inconsistency or ORM bug). Skipping heal prevents sequence corruption.
        Long invoiceCount = invoiceRepository.countBySellerIdAndPeriodYearAndPeriodMonth(
                sellerId, periodYear, periodMonth);
        if (invoiceCount == null || invoiceCount == 0) {
            log.error("CRITICAL phantom-MAX detected: findMaxSequenceNumber={} but invoiceCount=0 " +
                      "for sellerId={}, period={}/{}. Skipping heal to prevent sequence corruption.",
                      maxSeq, sellerId, periodYear, periodMonth);
            return;
        }

        int currentLastValue = repository
            .findBySellerIdAndResetPeriodAndPeriodKey(sellerId, resetPeriod, periodKey)
            .map(InvoiceNumberCounter::getLastValue)
            .orElse(0);

        if (maxSeq > currentLastValue) {
            log.warn("Counter drift detected — self-healing: sellerId={}, periodKey={}, counter={}, maxSeq={}. " +
                     "Healing to {}", sellerId, periodKey, currentLastValue, maxSeq, maxSeq);
            repository.healCounterIfBehind(sellerId, resetPeriod.name(), periodKey, fyStartYear, maxSeq);
        }
    }
}