package dev.robgro.timesheet.timesheet;

import java.time.LocalDate;

public record TimesheetDto(
        Long id,
        String clientName,
        LocalDate serviceDate,
        double duration,
        boolean invoiced,
        Long clientId,
        double hourlyRate,
        String invoiceNumber,
        LocalDate paymentDate
) {
}
