package dev.robgro.timesheet.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceItemDto(
        Long id,
        LocalDate serviceDate,
        String description,
        BigDecimal amount
) {}
