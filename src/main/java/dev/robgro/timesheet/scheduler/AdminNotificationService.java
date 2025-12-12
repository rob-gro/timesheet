package dev.robgro.timesheet.scheduler;

import java.util.List;

public interface AdminNotificationService {
    void sendErrorNotification(String subject, String details, Exception e);
    void sendSummaryNotification(InvoicingSummary summary);
    void sendEmptyClientWarning(List<String> clientNames);
}
