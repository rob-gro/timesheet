package dev.robgro.timesheet.scheduler;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Service for managing per-seller invoicing schedule configuration.
 */
public interface SellerScheduleConfigService {

    /**
     * Get (or lazily create) the schedule config for a seller.
     * Includes calculated {@code nextRunAt}.
     */
    SellerScheduleConfigDto getConfigForSeller(Long sellerId);

    /**
     * Save day/hour/timezone/enabled settings for a seller atomically
     * (config + seller timezone in one TX).
     */
    SellerScheduleConfigDto saveConfig(Long sellerId, SaveScheduleConfigRequest request);

    /**
     * Get all enabled configs with seller join-fetched (no N+1).
     * Used by the master CRON tick.
     */
    List<SellerScheduleConfig> getAllEnabledConfigs();

    /**
     * Calculate the next scheduled run time for a config.
     * Used by reservation and DTO mapping.
     */
    ZonedDateTime calculateNextRun(SellerScheduleConfig config);

    /**
     * Update run result after invoicing completes. Called outside the reservation TX.
     */
    void updateRunResult(Long configId, ScheduleRunStatus status, int success, int fail,
                         String errorSummary, LocalDateTime nowUtc);
}