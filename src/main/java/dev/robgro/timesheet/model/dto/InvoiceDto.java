package dev.robgro.timesheet.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceDto(
        Long id,
        String invoiceNumber,
        LocalDate issueDate,
        BigDecimal totalAmount,
        Long clientId,
        List<InvoiceItemDto> itemsIlst
) {}
