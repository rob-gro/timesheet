package dev.robgro.timesheet.invoice;

import java.math.BigDecimal;
import java.util.List;

public record InvoiceReportData(
        List<InvoiceDto> invoices,
        BigDecimal totalAmount,
        String period,
        String clientName
) {
}
