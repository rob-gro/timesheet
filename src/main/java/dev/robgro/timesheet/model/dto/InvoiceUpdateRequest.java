package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;
import java.util.List;

public record InvoiceUpdateRequest(
        Long clientId,
        LocalDate issueDate,
        String invoiceNumber,
        List<InvoiceItemUpdateRequest> items
) {
}
