package dev.robgro.timesheet.scheduler;

import java.time.LocalDateTime;

/**
 * Read-only view of a scheduler run and its delivery job statuses.
 */
public record RunStatusDto(
        String runId,
        String triggeredBy,
        LocalDateTime startedAt,
        long deliveryPending,
        long deliverySent,
        long deliveryFailed
) {}