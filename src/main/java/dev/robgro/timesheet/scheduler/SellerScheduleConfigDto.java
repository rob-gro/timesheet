package dev.robgro.timesheet.scheduler;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

/**
 * Read-only view of a {@link SellerScheduleConfig} for API/UI responses.
 * {@code nextRunAt} is calculated server-side by {@link SellerScheduleConfigService#calculateNextRun}.
 */
public record SellerScheduleConfigDto(
        Long id,
        Long sellerId,
        String sellerName,
        int scheduledDay,
        int scheduledHour,
        String timezone,
        boolean enabled,
        LocalDateTime lastRunAt,
        LocalDateTime runStartedAt,
        String lastRunPeriod,
        ScheduleRunStatus lastRunStatus,
        Integer lastRunSuccessCount,
        Integer lastRunFailCount,
        String lastErrorSummary,
        int runRetryCount,
        ZonedDateTime nextRunAt
) {}