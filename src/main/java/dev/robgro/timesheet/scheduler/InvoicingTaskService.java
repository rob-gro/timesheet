package dev.robgro.timesheet.scheduler;

import java.time.YearMonth;

public interface InvoicingTaskService {

    InvoicingSummary executeMonthlyInvoicing(Long sellerId, YearMonth period, String runId);

    /** Backward-compatible overload — delegates with runId=null. */
    default InvoicingSummary executeMonthlyInvoicing(Long sellerId, YearMonth period) {
        return executeMonthlyInvoicing(sellerId, period, null);
    }
}