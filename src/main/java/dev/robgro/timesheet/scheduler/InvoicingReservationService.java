package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static dev.robgro.timesheet.scheduler.ScheduleRunStatus.*;

/**
 * Handles atomic reservation of invoicing runs.
 *
 * <p>PESSIMISTIC_WRITE lock in {@link #tryReserve} is held for the shortest possible time
 * (just the reservation — markInProgress + save). Actual invoicing runs outside the TX.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingReservationService {

    private final SellerScheduleConfigRepository configRepository;
    private final SellerScheduleConfigService configService;
    private final Clock clock;

    /**
     * Check whether the scheduler should fire for this seller right now.
     *
     * <p>Guards:
     * <ol>
     *   <li>Idempotency: last billing period already SUCCESS or PARTIAL_FAIL → skip</li>
     *   <li>Retry cap: FAIL but runRetryCount ≥ 1 → skip</li>
     *   <li>Stuck detection: IN_PROGRESS > stuckTimeoutMinutes → override</li>
     *   <li>Day/hour check: must reach configured day and hour in the seller's timezone</li>
     * </ol>
     */
    public boolean shouldRunForSeller(SellerScheduleConfig config, ZonedDateTime now) {
        YearMonth previousMonth = YearMonth.from(now).minusMonths(1);
        String previousPeriod = previousMonth.toString();

        if (previousPeriod.equals(config.getLastRunPeriod())) {
            ScheduleRunStatus status = config.getLastRunStatus();
            if (status == SUCCESS || status == PARTIAL_FAIL) {
                return false;
            }
            if (status == FAIL) {
                if (config.getRunRetryCount() >= 1) {
                    return false;
                }
                // runRetryCount == 0 → allow one retry (falls through to day/hour check)
            }
            // Stuck run detection: IN_PROGRESS for more than 90 min on same period
            if (status == IN_PROGRESS) {
                if (config.getRunStartedAt() != null
                        && config.getRunStartedAt()
                                .isBefore(LocalDateTime.now(clock).minusMinutes(90))) {
                    log.warn("Detected stuck IN_PROGRESS run for seller '{}', period={}, runStartedAt={}. Overriding.",
                            config.getSeller().getName(), previousPeriod, config.getRunStartedAt());
                    // Allow override — falls through to day/hour check
                } else {
                    return false; // Run still in progress or stuck on different period
                }
            }
        }

        // Clamp scheduled day to the billing month (previous month)
        int effectiveDay = Math.min(config.getScheduledDay(), previousMonth.lengthOfMonth());

        // Option B: fire on first hourly tick >= scheduledDay AND >= scheduledHour
        return now.getDayOfMonth() >= effectiveDay
                && now.getHour() >= config.getScheduledHour();
    }

    /**
     * Atomically reserve a run for the given config.
     * Returns empty if the double-check under lock fails (already processed by another instance).
     *
     * <p>TX is SHORT: lock → markInProgress → save → commit. Invoicing is NOT inside this TX.
     */
    @Transactional
    public Optional<RunContext> tryReserve(Long configId) {
        SellerScheduleConfig config = configRepository.findByIdWithLock(configId).orElseThrow();

        // Double-check under lock — another instance may have processed this between tick and lock
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(config.getSeller().getTimezone()));
        if (!shouldRunForSeller(config, now)) {
            log.debug("Double-check failed for config {} — already processed, skipping", configId);
            return Optional.empty();
        }
        if (!config.isEnabled()) {
            log.info("Config {} disabled between tick check and lock — skipping", configId);
            return Optional.empty();
        }

        return Optional.of(doReserve(config, now, false));
    }

    /**
     * Force-reserve a run (admin-triggered). Bypasses day/hour guard.
     */
    @Transactional
    public RunContext forceReserve(Long configId) {
        SellerScheduleConfig config = configRepository.findByIdWithLock(configId).orElseThrow();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(config.getSeller().getTimezone()));
        return doReserve(config, now, false);
    }

    // ===== Private =====

    private RunContext doReserve(SellerScheduleConfig config, ZonedDateTime now, boolean forceOverride) {
        YearMonth previousMonth = YearMonth.from(now).minusMonths(1);
        String period = previousMonth.toString();
        String runId = UUID.randomUUID().toString();
        LocalDateTime nowUtc = LocalDateTime.now(clock);

        boolean isRetry = period.equals(config.getLastRunPeriod())
                && config.getLastRunStatus() == FAIL
                && config.getRunRetryCount() == 0;

        // calculateNextRun BEFORE markInProgress (after it, next run shifts to following month)
        ZonedDateTime scheduledTime = configService.calculateNextRun(config);

        config.markInProgress(period, isRetry, nowUtc);
        configRepository.save(config);

        log.info("[runId={}] Reserved invoicing for seller '{}', period={}, isRetry={}",
                runId, config.getSeller().getName(), period, isRetry);

        return new RunContext(config.getId(), config.getSeller(), previousMonth, runId, isRetry, scheduledTime);
    }
}