package dev.robgro.timesheet.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceItemDto(
        Long id,
        LocalDate serviceDate,
        String description,
        double duration,
        BigDecimal amount,
        double hourlyRate
) {
}
