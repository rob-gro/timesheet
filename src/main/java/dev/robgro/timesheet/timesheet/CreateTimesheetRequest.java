package dev.robgro.timesheet.timesheet;

import java.time.LocalDate;

public record CreateTimesheetRequest(
        Long clientId,
        LocalDate serviceDate,
        double duration,
        Boolean isPaidAlready
) {
}
