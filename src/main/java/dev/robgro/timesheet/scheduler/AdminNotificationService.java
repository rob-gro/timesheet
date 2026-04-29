package dev.robgro.timesheet.scheduler;

import java.time.YearMonth;
import java.util.List;

public interface AdminNotificationService {
    void sendErrorNotification(String subject, String details, Exception e);
    void sendSummaryNotification(InvoicingSummary summary);
    void sendEmptyClientWarning(List<String> clientNames);
    void sendTenantScheduleErrorNotification(
            String tenantEmail, YearMonth month, ScheduleRunStatus status,
            int success, int total, String runId);
}