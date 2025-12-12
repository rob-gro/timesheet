package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "scheduling.invoicing.enabled", havingValue = "true")
public class InvoicingScheduler {

    private final InvoicingTaskService invoicingTaskService;
    private final AdminNotificationService notificationService;

    @Scheduled(cron = "${scheduling.invoicing.cron}")
    public void generateMonthlyInvoices() {
        log.info("▶ Starting scheduled task: monthly invoice generation");

        try {
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing();

            log.info("✅ Scheduled task completed successfully. Generated {} invoices",
                    summary.getTotalInvoices());

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR in scheduled task: {}", e.getMessage(), e);

            notificationService.sendErrorNotification(
                    "CRITICAL: Scheduled invoicing task failed",
                    "The entire automated invoicing process failed with an error.",
                    e
            );

            throw new RuntimeException("Scheduled invoicing task failed", e);
        }
    }
}
