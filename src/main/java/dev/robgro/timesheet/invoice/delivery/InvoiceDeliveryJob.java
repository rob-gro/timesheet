package dev.robgro.timesheet.invoice.delivery;

import dev.robgro.timesheet.invoice.Invoice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Async delivery job for an invoice (PDF generation + FTP upload + email send).
 *
 * <p>State machine:
 * <pre>
 * PENDING → PROCESSING → SENT         (success)
 * PENDING → PROCESSING → PENDING      (retryable failure, is_active stays 1)
 * PENDING → PROCESSING → FAILED       (terminal failure, is_active → NULL)
 * </pre>
 *
 * <p>is_active invariant (enforced by entity, backed by DB UNIQUE constraint):
 * <ul>
 *   <li>PENDING, PROCESSING → is_active = 1</li>
 *   <li>SENT, FAILED        → is_active = NULL</li>
 * </ul>
 */
@Entity
@Table(name = "invoice_delivery_job")
@Getter
@NoArgsConstructor
@Slf4j
public class InvoiceDeliveryJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "run_id", length = 36)
    private String runId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "delivery_hash", length = 64)
    private String deliveryHash;

    @Column(name = "manual", nullable = false)
    private boolean manual = false;

    /**
     * is_active = 1 for PENDING/PROCESSING (active in queue).
     * is_active = NULL for SENT/FAILED (historical — removed from queue).
     * Using Integer (not boolean) to represent the nullable DB column.
     */
    @Column(name = "is_active")
    private Integer isActive = 1;

    // ===== Factory methods =====

    public static InvoiceDeliveryJob createPending(Invoice invoice) {
        InvoiceDeliveryJob job = new InvoiceDeliveryJob();
        job.invoice = invoice;
        job.status = DeliveryStatus.PENDING.name();
        job.isActive = 1;
        job.attempts = 0;
        job.nextRetryAt = LocalDateTime.now();
        job.createdAt = LocalDateTime.now();
        job.updatedAt = LocalDateTime.now();
        return job;
    }

    public static InvoiceDeliveryJob createManualResend(Invoice invoice) {
        InvoiceDeliveryJob job = createPending(invoice);
        job.manual = true;
        return job;
    }

    // ===== State transitions =====

    /**
     * Mark delivery as successful.
     * is_active → NULL (removed from active queue).
     * delivery_hash written HERE — never at claim time.
     */
    public void markSent(String deliveryHash) {
        assertStatus(DeliveryStatus.PROCESSING);
        this.status = DeliveryStatus.SENT.name();
        this.isActive = null;
        this.deliveryHash = deliveryHash;
        this.claimToken = null;
        this.lockedAt = null;
        this.lockedBy = null;
        this.updatedAt = LocalDateTime.now();
        assertIsActiveInvariant();
    }

    /**
     * Mark delivery as failed but retryable.
     * status → PENDING, is_active stays 1 (job returns to queue).
     * attempts incremented BEFORE backoff calculation (caller uses attempts-1 for index).
     */
    public void markRetryableFailure(String error, LocalDateTime nextRetryAt) {
        assertStatus(DeliveryStatus.PROCESSING);
        this.attempts++;
        this.status = DeliveryStatus.PENDING.name();
        this.isActive = 1;
        this.lastError = truncate(error, 65535);
        this.nextRetryAt = nextRetryAt;
        this.lockedAt = null;
        this.lockedBy = null;
        this.claimToken = null;
        this.updatedAt = LocalDateTime.now();
        assertIsActiveInvariant();
    }

    /**
     * Mark delivery as terminally failed (no more retries).
     * is_active → NULL (removed from active queue permanently).
     */
    public void markTerminalFailed(String error) {
        assertStatus(DeliveryStatus.PROCESSING);
        this.attempts++;
        this.status = DeliveryStatus.FAILED.name();
        this.isActive = null;
        this.lastError = truncate(error, 65535);
        this.lockedAt = null;
        this.lockedBy = null;
        this.claimToken = null;
        this.updatedAt = LocalDateTime.now();
        assertIsActiveInvariant();
    }

    /**
     * Reset a FAILED job for manual retry.
     * is_active → 1 (back to active queue).
     * delivery_hash cleared so idempotency check doesn't block new send.
     */
    public void resetForRetry() {
        if (!DeliveryStatus.FAILED.name().equals(this.status)) {
            throw new IllegalStateException(
                    "Can only reset FAILED jobs, current status: " + this.status);
        }
        this.status = DeliveryStatus.PENDING.name();
        this.isActive = 1;
        this.deliveryHash = null;
        this.lockedAt = null;
        this.lockedBy = null;
        this.claimToken = null;
        this.nextRetryAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canRetry(int maxAttempts) {
        return attempts < maxAttempts;
    }

    public boolean isManual() {
        return manual;
    }

    /**
     * Associate this job with a scheduler run for status tracking.
     * Called by {@link dev.robgro.timesheet.scheduler.InvoicingTaskServiceImpl}
     * after invoice creation; does NOT affect delivery semantics.
     */
    public void assignToRun(String runId) {
        this.runId = runId;
        this.updatedAt = LocalDateTime.now();
    }

    // ===== Invariant checks =====

    private void assertStatus(DeliveryStatus expected) {
        if (!expected.name().equals(this.status)) {
            throw new IllegalStateException(
                    "Expected status " + expected + " but was " + this.status
                            + " for job id=" + this.id);
        }
    }

    private void assertIsActiveInvariant() {
        boolean activeShouldBe1 = DeliveryStatus.PENDING.name().equals(status)
                || DeliveryStatus.PROCESSING.name().equals(status);
        boolean isActiveIs1 = Integer.valueOf(1).equals(isActive);

        if (activeShouldBe1 && !isActiveIs1) {
            throw new IllegalStateException(
                    "is_active invariant violated: status=" + status + " requires is_active=1 but got " + isActive);
        }
        if (!activeShouldBe1 && isActive != null) {
            throw new IllegalStateException(
                    "is_active invariant violated: status=" + status + " requires is_active=NULL but got " + isActive);
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}