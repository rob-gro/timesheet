package dev.robgro.timesheet.invoice.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link InvoiceDeliveryJob}.
 *
 * <p>Atomic claiming is handled by {@link DeliveryClaimService} via JDBC TransactionTemplate
 * (READ_COMMITTED isolation). This repository is used for JPA reads and standard updates only.
 */
@Repository
public interface InvoiceDeliveryJobRepository extends JpaRepository<InvoiceDeliveryJob, Long> {

    /**
     * Find the currently active job for an invoice (is_active = 1 → PENDING or PROCESSING).
     * At most one active job per invoice (enforced by UNIQUE INDEX uniq_invoice_active).
     */
    Optional<InvoiceDeliveryJob> findByInvoiceIdAndIsActive(Long invoiceId, Integer isActive);

    /**
     * Find the most recent job for an invoice regardless of status.
     * Used by requestDelivery() to inspect terminal (SENT/FAILED) jobs.
     */
    Optional<InvoiceDeliveryJob> findTopByInvoiceIdOrderByCreatedAtDesc(Long invoiceId);

    /**
     * Find jobs claimed in the current worker tick, ordered by id for deterministic processing.
     * Called after JDBC claim UPDATE to fetch the exact rows that were updated.
     */
    List<InvoiceDeliveryJob> findByClaimTokenOrderByIdAsc(String claimToken);

    /**
     * Stale lock recovery: reset PROCESSING jobs whose lock is older than cutoff.
     *
     * <p>attempts NOT incremented — we don't know if I/O actually ran.
     * Lock is cleared and job returns to PENDING queue.
     * last_error records when the stale lock was detected.
     *
     * <p>AND locked_at IS NOT NULL: prevents recovery of a job stuck without a valid lock.
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE invoice_delivery_job
            SET status      = 'PENDING',
                locked_at   = NULL,
                locked_by   = NULL,
                claim_token = NULL,
                last_error  = CONCAT('stale-lock at ', NOW(), ': ', COALESCE(last_error, '')),
                updated_at  = NOW()
            WHERE status = 'PROCESSING'
              AND locked_at IS NOT NULL
              AND locked_at < :cutoff
            """, nativeQuery = true)
    int recoverStaleLocks(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Cancel active delivery jobs for a cancelled invoice.
     * Moves PENDING/PROCESSING (is_active=1) to terminal FAILED (is_active=NULL).
     */
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE invoice_delivery_job
            SET status      = 'FAILED',
                is_active   = NULL,
                last_error  = 'Invoice cancelled',
                claim_token = NULL,
                locked_at   = NULL,
                locked_by   = NULL,
                updated_at  = NOW()
            WHERE invoice_id = :invoiceId
              AND is_active = 1
            """, nativeQuery = true)
    int cancelJobsForInvoice(@Param("invoiceId") Long invoiceId);

    /**
     * Bridge method: converts boolean isActive to Integer for callers that pass true/false.
     * true → is_active = 1 (PENDING/PROCESSING); false → is_active = NULL (terminal).
     */
    default Optional<InvoiceDeliveryJob> findByInvoice_IdAndIsActive(Long invoiceId, boolean isActive) {
        return findByInvoiceIdAndIsActive(invoiceId, isActive ? 1 : null);
    }

    /**
     * Count delivery jobs by scheduler runId and status.
     * Used by RunStatusService to aggregate per-run delivery statistics.
     */
    @Query(value = "SELECT COUNT(*) FROM invoice_delivery_job WHERE run_id = :runId AND status = :status",
            nativeQuery = true)
    long countByRunIdAndStatus(@Param("runId") String runId, @Param("status") String status);

    /**
     * Check if a delivery with the given hash was already sent for this invoice.
     * Used for crash-recovery idempotency (skip re-send if SENT record found).
     */
    @Query(value = """
            SELECT COUNT(*) FROM invoice_delivery_job
            WHERE invoice_id = :invoiceId
              AND delivery_hash = :hash
              AND status = 'SENT'
            """, nativeQuery = true)
    long countSentByInvoiceIdAndHash(
            @Param("invoiceId") Long invoiceId,
            @Param("hash") String hash
    );
}