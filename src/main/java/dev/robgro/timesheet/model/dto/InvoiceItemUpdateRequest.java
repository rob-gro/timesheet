package dev.robgro.timesheet.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceItemUpdateRequest(
        Long id,
        Long timesheetId,
        LocalDate serviceDate,
        String description,
        double duration,
        BigDecimal amount
) {
}
