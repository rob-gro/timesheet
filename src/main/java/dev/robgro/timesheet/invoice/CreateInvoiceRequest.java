package dev.robgro.timesheet.invoice;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(

        Long clientId,
        @NotNull(message = "Seller ID is required")
        Long sellerId,
        LocalDate issueDate,
        List<Long> timesheetIds,
        String invoiceNumber
) {
}
