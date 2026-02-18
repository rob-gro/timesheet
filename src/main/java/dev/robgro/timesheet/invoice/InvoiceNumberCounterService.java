package dev.robgro.timesheet.invoice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for atomic invoice number sequence generation.
 *
 * <p>Uses MySQL UPSERT pattern to atomically increment counters without race conditions.
 * Thread-safe: Multiple concurrent requests for same scope will get unique sequence numbers.
 *
 * <p>Self-healing: Before each increment, checks MAX(sequence_number) in invoices table.
 * If counter is behind (drift detected), heals automatically using GREATEST() UPSERT.
 * This prevents duplicate number collisions caused by:
 * - V27 backfill bugs (COUNT vs MAX)
 * - Race conditions with transaction rollbacks
 * - Manual database edits
 *
 * <p>Contract:
 * - Input: sellerId, resetPeriod, periodKey, fyStartYear, periodYear, periodMonth
 * - Output: next sequence number (1, 2, 3, ...)
 * - Thread-safe: atomic increment via UPSERT
 * - Self-healing: counter drift corrected automatically before increment
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
     * </ol>
     *
     * <p>Example — drift scenario:
     * <pre>
     * invoices: seq 1, 2 (counter somehow lost update for seq=2)
     * counter.lastValue = 1
     * → self-healing bumps counter to 2
     * → nextSequence returns 3 (no collision)
     * </pre>
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

        // Self-healing: detect and correct counter drift before issuing next number
        healIfDrifted(sellerId, resetPeriod, periodKey, fyStartYear, periodYear, periodMonth);

        // Atomic UPSERT: INSERT or UPDATE + set LAST_INSERT_ID
        repository.bumpAndSetLastInsertId(sellerId, resetPeriod.name(), periodKey, fyStartYear);

        // Retrieve the value set by LAST_INSERT_ID() in same transaction
        long sequence = repository.lastInsertId();

        log.debug("Generated sequence: {} for sellerId={}, periodKey={}", sequence, sellerId, periodKey);
        return (int) sequence;
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

        return repository.findBySellerIdAndResetPeriodAndPeriodKey(sellerId, resetPeriod, periodKey)
            .map(counter -> counter.getLastValue() + 1)
            .orElse(1);
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
            return; // No invoices yet — nothing to heal
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