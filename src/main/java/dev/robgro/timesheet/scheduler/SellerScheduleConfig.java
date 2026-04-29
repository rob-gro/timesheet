package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.seller.Seller;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static dev.robgro.timesheet.scheduler.ScheduleRunStatus.*;

/**
 * Per-seller invoicing schedule configuration.
 *
 * <p>Idempotency key: {@code lastRunPeriod} (format "YYYY-MM") — independent of timezone/DST.
 *
 * <p>Retry logic:
 * <ul>
 *   <li>{@code runRetryCount == 0} — no retry yet; one retry is allowed</li>
 *   <li>{@code runRetryCount == 1} — retry already used; no more retries for this period</li>
 * </ul>
 */
@Entity
@Table(name = "seller_schedule_config")
@Getter
@Setter
@NoArgsConstructor
public class SellerScheduleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(name = "scheduled_day", nullable = false)
    private int scheduledDay = 1;

    @Column(name = "scheduled_hour", nullable = false)
    private int scheduledHour = 12;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "run_started_at")
    private LocalDateTime runStartedAt;

    /** YYYY-MM — main idempotency guard, independent of timezone/DST. */
    @Column(name = "last_run_period", length = 7)
    private String lastRunPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_run_status", length = 20, columnDefinition = "VARCHAR(20)")
    private ScheduleRunStatus lastRunStatus;

    @Column(name = "last_run_success_count")
    private Integer lastRunSuccessCount;

    @Column(name = "last_run_fail_count")
    private Integer lastRunFailCount;

    @Column(name = "last_error_summary", columnDefinition = "TEXT")
    private String lastErrorSummary;

    /**
     * 0 = no retry yet for current period; 1 = retry already used.
     * Reset to 0 by {@link #markInProgress} when starting a new period.
     */
    @Column(name = "run_retry_count", nullable = false)
    private int runRetryCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ===== Domain methods =====

    /**
     * Mark run as reserved/in-progress. Call under PESSIMISTIC_WRITE lock before releasing.
     *
     * <p>{@code runRetryCount} is incremented HERE (at reservation time), not in
     * {@link #updateRunResult}. Semantics: once a retry is reserved, the gate is closed.
     *
     * @param period  billing period string "YYYY-MM"
     * @param isRetry true if this is the auto-retry after a FAIL for the same period
     * @param nowUtc  current UTC time (injected from service's Clock — deterministic for tests)
     */
    public void markInProgress(String period, boolean isRetry, LocalDateTime nowUtc) {
        if (isRetry) {
            this.runRetryCount = 1;  // explicit assignment — clearer than ++ on MVP
        } else {
            this.runRetryCount = 0;  // new period or fresh start
        }
        this.lastRunPeriod = period;
        this.lastRunStatus = IN_PROGRESS;
        this.runStartedAt = nowUtc;
        this.lastRunSuccessCount = null;
        this.lastRunFailCount = null;
        this.lastErrorSummary = null;
    }

    /**
     * Record run result. Called after invoicing completes (outside the reservation TX).
     * Does NOT touch {@code runRetryCount} — that is managed exclusively by {@link #markInProgress}.
     */
    public void updateRunResult(ScheduleRunStatus status, int success, int fail,
                                String errorSummary, LocalDateTime nowUtc) {
        this.lastRunAt = nowUtc;
        this.lastRunStatus = status;
        this.lastRunSuccessCount = success;
        this.lastRunFailCount = fail;
        this.lastErrorSummary = errorSummary;
    }
}