package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;

public record TimesheetDto(
        Long id,
        String clientName,
        LocalDate serviceDate,
        double duration,
        boolean isInvoice,
        Long clientId,
        double hourlyRate,
        String invoiceNumber

) {
}
