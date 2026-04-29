package dev.robgro.timesheet.scheduler;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Input request for saving invoicing schedule configuration.
 */
public record SaveScheduleConfigRequest(
        @Min(1) @Max(31) int scheduledDay,
        @Min(0) @Max(23) int scheduledHour,
        @NotBlank String timezone,
        boolean enabled
) {}