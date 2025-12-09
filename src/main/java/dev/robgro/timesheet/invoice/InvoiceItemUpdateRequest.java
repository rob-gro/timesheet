package dev.robgro.timesheet.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceItemUpdateRequest(
        Long id,
        Long timesheetId,
        LocalDate serviceDate,
        String description,
        double duration,
        BigDecimal amount,
        double hourlyRate
) {
}
