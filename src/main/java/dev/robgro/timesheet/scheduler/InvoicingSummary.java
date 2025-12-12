package dev.robgro.timesheet.scheduler;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Data
@Builder
public class InvoicingSummary {
    private LocalDateTime executionTime;
    private YearMonth previousMonth;
    private int totalInvoices;
    private int successfulInvoices;
    private int failedInvoices;
    private List<String> clientsWithoutTimesheets;
    private List<InvoiceProcessingResult> processingResults;
}
