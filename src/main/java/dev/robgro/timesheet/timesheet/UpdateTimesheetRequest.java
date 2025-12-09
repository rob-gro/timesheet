package dev.robgro.timesheet.timesheet;

import java.time.LocalDate;

public record UpdateTimesheetRequest(
        Long clientId,
        LocalDate serviceDate,
        double duration,
        Boolean isPaidAlready
) {
}
