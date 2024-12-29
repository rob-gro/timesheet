package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;

public record CreateTimesheetRequest(
        Long clientId,
        LocalDate serviceDate,
        double duration
) {
}
