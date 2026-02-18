package dev.robgro.timesheet.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository for atomic invoice number counter operations.
 *
 * <p>Uses MySQL UPSERT pattern (INSERT ... ON DUPLICATE KEY UPDATE)
 * to atomically increment sequence numbers without race conditions.
 *
 * <p>Thread-safe: Multiple concurrent requests for same scope
 * (seller_id, reset_period, period_key) will get unique sequence numbers.
 */
@Repository
public interface InvoiceNumberCounterRepository extends JpaRepository<InvoiceNumberCounter, Long> {

    /**
     * Atomically increment counter and set LAST_INSERT_ID() for retrieval.
     *
     * <p>UPSERT behavior:
     * - First invoice: INSERT with last_value=LAST_INSERT_ID(1), sets LAST_INSERT_ID=1
     * - Subsequent: UPDATE last_value=LAST_INSERT_ID(last_value+1), returns new value
     *
     * <p>CRITICAL: Must use {@code LAST_INSERT_ID(1)} in INSERT to ensure
     * first sequence returns 1 (not auto_increment ID). Correctness > hygiene.
     *
     * @param sellerId Tenant ID (multi-tenant isolation)
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodKey Period identifier matching V27 format (e.g., "2026-02")
     * @param fyStartYear Fiscal year start (currently unused, pass null)
     * @return Number of rows affected (1 = success)
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value, fy_start_year)
        VALUES (:sellerId, :resetPeriod, :periodKey, LAST_INSERT_ID(1), :fyStartYear)
        ON DUPLICATE KEY UPDATE last_value = LAST_INSERT_ID(last_value + 1)
        """, nativeQuery = true)
    int bumpAndSetLastInsertId(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") String resetPeriod,
        @Param("periodKey") String periodKey,
        @Param("fyStartYear") Integer fyStartYear
    );

    /**
     * Retrieve the value set by LAST_INSERT_ID() in previous UPSERT call.
     *
     * <p>MUST be called in same transaction as {@link #bumpAndSetLastInsertId}.
     * Returns the newly generated sequence number.
     *
     * @return Sequence number (1, 2, 3, ...)
     */
    @Query(value = "SELECT LAST_INSERT_ID()", nativeQuery = true)
    long lastInsertId();

    /**
     * Find all counters for a seller with stable ordering.
     *
     * <p>Used by observability endpoint to display counter status.
     * Ordering: reset_period ASC, period_key DESC (newest periods first).
     *
     * @param sellerId Tenant ID
     * @return List of counters sorted by reset period and period key
     */
    List<InvoiceNumberCounter> findBySellerIdOrderByResetPeriodAscPeriodKeyDesc(Long sellerId);

    /**
     * Find counter for given scope WITHOUT incrementing it.
     *
     * <p>Used by preview to show next invoice number without reserving it.
     *
     * @param sellerId Tenant ID
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodKey Period identifier (e.g., "2026-02")
     * @return Counter if exists, empty if first invoice in scope
     */
    /**
     * Heal counter if it is behind the actual invoice data.
     *
     * <p>Uses GREATEST() to ensure counter only moves forward — never backwards.
     * Safe for concurrent calls: if two threads both try to heal, both will succeed
     * with no negative effect (idempotent).
     *
     * <p>Handles both cases:
     * <ul>
     *   <li>Counter exists but behind: UPDATE sets last_value = GREATEST(current, minValue)</li>
     *   <li>Counter missing: INSERT with last_value = minValue</li>
     * </ul>
     *
     * @param sellerId Tenant ID
     * @param resetPeriod Reset strategy as string (e.g., "MONTHLY")
     * @param periodKey Period identifier (e.g., "2026-02")
     * @param fyStartYear Fiscal year start (pass null if unused)
     * @param minValue Minimum value counter must have (MAX(sequence_number) from invoices)
     */
    @Modifying
    @Transactional
    @Query(value = """
        INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value, fy_start_year)
        VALUES (:sellerId, :resetPeriod, :periodKey, :minValue, :fyStartYear)
        ON DUPLICATE KEY UPDATE last_value = GREATEST(last_value, :minValue)
        """, nativeQuery = true)
    void healCounterIfBehind(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") String resetPeriod,
        @Param("periodKey") String periodKey,
        @Param("fyStartYear") Integer fyStartYear,
        @Param("minValue") int minValue
    );

    @Query("""
        SELECT c FROM InvoiceNumberCounter c
        WHERE c.sellerId = :sellerId
        AND c.resetPeriod = :resetPeriod
        AND c.periodKey = :periodKey
        """)
    java.util.Optional<InvoiceNumberCounter> findBySellerIdAndResetPeriodAndPeriodKey(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") ResetPeriod resetPeriod,
        @Param("periodKey") String periodKey
    );
}
