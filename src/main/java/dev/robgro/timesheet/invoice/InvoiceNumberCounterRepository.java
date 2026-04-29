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
 * <p>Uses MySQL/MariaDB UPSERT pattern (INSERT ... ON DUPLICATE KEY UPDATE)
 * to atomically increment sequence numbers without race conditions.
 *
 * <p>Thread-safe: Multiple concurrent requests for same scope
 * (seller_id, reset_period, period_key) will get unique sequence numbers.
 *
 * <p>Fix 2026-03: Removed LAST_INSERT_ID() pattern which returned the auto_increment row ID
 * on fresh INSERT instead of the sequence value. Now uses bumpCounter() + findLastValue()
 * to reliably read the actual last_value column after increment.
 */
@Repository
public interface InvoiceNumberCounterRepository extends JpaRepository<InvoiceNumberCounter, Long> {

    /**
     * Atomically increment counter (MariaDB-safe, no LAST_INSERT_ID).
     *
     * <p>UPSERT behavior:
     * - First invoice: INSERT with last_value=1
     * - Subsequent: UPDATE last_value = last_value + 1
     *
     * <p>After this call, use {@link #findLastValue} to read the current sequence number.
     *
     * @param sellerId Tenant ID (multi-tenant isolation)
     * @param resetPeriod Reset strategy (MONTHLY, YEARLY, NEVER)
     * @param periodKey Period identifier matching V27 format (e.g., "2026-02")
     * @param fyStartYear Fiscal year start (currently unused, pass null)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        INSERT INTO invoice_number_counters (seller_id, reset_period, period_key, last_value, fy_start_year)
        VALUES (:sellerId, :resetPeriod, :periodKey, 1, :fyStartYear)
        ON DUPLICATE KEY UPDATE last_value = last_value + 1
        """, nativeQuery = true)
    void bumpCounter(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") String resetPeriod,
        @Param("periodKey") String periodKey,
        @Param("fyStartYear") Integer fyStartYear
    );

    /**
     * Read the current last_value for a counter scope directly from the table.
     *
     * <p>Must be called in same transaction as {@link #bumpCounter} to get
     * the atomically incremented value.
     *
     * @param sellerId Tenant ID
     * @param resetPeriod Reset strategy as string (e.g., "MONTHLY")
     * @param periodKey Period identifier (e.g., "2026-02")
     * @return Current last_value, or null if counter row does not exist
     */
    @Query(value = """
        SELECT last_value
        FROM invoice_number_counters
        WHERE seller_id = :sellerId
          AND reset_period = :resetPeriod
          AND period_key = :periodKey
        """, nativeQuery = true)
    Integer findLastValue(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") String resetPeriod,
        @Param("periodKey") String periodKey
    );

    /**
     * Find all counters for a seller with stable ordering.
     *
     * <p>Used by observability endpoint to display counter status.
     * Ordering: period_key DESC (newest periods first).
     *
     * @param sellerId Tenant ID
     * @return List of counters sorted by period key descending
     */
    List<InvoiceNumberCounter> findBySellerIdOrderByPeriodKeyDesc(Long sellerId);

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
    @Modifying(clearAutomatically = true, flushAutomatically = true)
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

    /**
     * Find counter entity for given scope (JPQL, returns entity with all fields).
     *
     * <p>Used by heal logic to read current lastValue for drift detection.
     */
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

    /**
     * Delete a specific counter row (used in integration test cleanup).
     *
     * @param sellerId Tenant ID
     * @param resetPeriod Reset strategy as string (e.g., "MONTHLY")
     * @param periodKey Period identifier (e.g., "2099-01")
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
        DELETE FROM invoice_number_counters
        WHERE seller_id = :sellerId
          AND reset_period = :resetPeriod
          AND period_key = :periodKey
        """, nativeQuery = true)
    void deleteCounter(
        @Param("sellerId") Long sellerId,
        @Param("resetPeriod") String resetPeriod,
        @Param("periodKey") String periodKey
    );
}