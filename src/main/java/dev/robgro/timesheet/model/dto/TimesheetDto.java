package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;

public record TimesheetDto(
        Long id,
        LocalDate serviceDate,
        double duration,
        boolean isInvoice,
        Long clientId,
        double hourlyRate
) {
}
