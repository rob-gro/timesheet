package dev.robgro.timesheet.invoice;

import java.time.LocalDate;
import java.util.List;

public record CreateInvoiceRequest(

        Long clientId,
        LocalDate issueDate,
        List<Long> timesheetIds,
        String invoiceNumber
) {
}
