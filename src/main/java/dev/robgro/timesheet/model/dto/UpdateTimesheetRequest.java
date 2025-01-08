package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;

public record UpdateTimesheetRequest(
        Long clientId,
        LocalDate serviceDate,
        double duration
) {
}
