package dev.robgro.timesheet.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

import static dev.robgro.timesheet.scheduler.ScheduleRunStatus.*;

/**
 * Executes an invoicing run and records the result.
 *
 * <p>No {@code @Transactional} wrapping the entire execution — each invoice
 * in {@link InvoicingTaskService} has its own TX. A single large TX would:
 * <ul>
 *   <li>Hold the DB connection for the entire batch duration</li>
 *   <li>Risk Heroku's 30s request timeout on cold dynos</li>
 *   <li>Prevent other sellerScheduleConfig updates from being visible mid-run</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingExecutionService {

    private final InvoicingTaskService invoicingTaskService;
    private final SellerScheduleConfigService configService;
    private final AdminNotificationService notificationService;
    private final Clock clock;

    /**
     * Execute invoicing and record the result in the config.
     * Must be called OUTSIDE the reservation TX (lock released before this runs).
     */
    public void executeAndRecord(RunContext ctx) {
        MDC.put("runId", ctx.runId());
        MDC.put("sellerId", String.valueOf(ctx.seller().getId()));

        try {
            log.info("[runId={}] Starting invoicing for seller '{}', period={}, isRetry={}",
                    ctx.runId(), ctx.seller().getName(), ctx.period(), ctx.isRetry());

            // CRITICAL: pass sellerId — filters clients for THIS seller only.
            // Without this filter, all tenants' invoices would be created at once.
            InvoicingSummary summary = invoicingTaskService.executeMonthlyInvoicing(
                    ctx.seller().getId(), ctx.period(), ctx.runId());

            ScheduleRunStatus status = determineStatus(summary);
            LocalDateTime nowUtc = LocalDateTime.now(clock);

            configService.updateRunResult(ctx.configId(), status,
                    summary.successfulInvoices(), summary.failedInvoices(),
                    status != SUCCESS ? buildErrorSummary(summary) : null, nowUtc);

            log.info("[runId={}] Completed: status={}, {}/{} invoices",
                    ctx.runId(), status, summary.successfulInvoices(), summary.totalInvoices());

            if (status != SUCCESS && !ctx.isRetry()) {
                sendFailureNotifications(ctx, summary, status);
            } else if (status != SUCCESS) {
                log.info("[runId={}] Retry also failed — skipping notification (already sent on first attempt)",
                        ctx.runId());
            }

        } catch (Exception e) {
            log.error("[runId={}] CRITICAL ERROR for seller '{}': {}",
                    ctx.runId(), ctx.seller().getName(), e.getMessage(), e);

            configService.updateRunResult(ctx.configId(), FAIL, 0, 0,
                    e.getMessage(), LocalDateTime.now(clock));

            if (!ctx.isRetry()) {
                notificationService.sendErrorNotification(
                        "CRITICAL: Invoicing failed – seller: " + ctx.seller().getName()
                                + " [runId=" + ctx.runId() + "]",
                        "Period: " + ctx.period() + "\nSeller ID: " + ctx.seller().getId(),
                        e);
            } else {
                log.warn("[runId={}] Retry also threw exception — skipping admin notification", ctx.runId());
            }
        } finally {
            MDC.remove("runId");
            MDC.remove("sellerId");
        }
    }

    // ===== Helpers =====

    private ScheduleRunStatus determineStatus(InvoicingSummary summary) {
        if (summary.failedInvoices() == 0) return SUCCESS;
        if (summary.successfulInvoices() > 0) return PARTIAL_FAIL;
        return FAIL;
    }

    private String buildErrorSummary(InvoicingSummary summary) {
        return String.format("%d/%d invoices failed",
                summary.failedInvoices(), summary.totalInvoices());
    }

    private void sendFailureNotifications(RunContext ctx, InvoicingSummary summary, ScheduleRunStatus status) {
        String tenantEmail = ctx.seller().getEmail();
        if (tenantEmail != null && !tenantEmail.isBlank()) {
            notificationService.sendTenantScheduleErrorNotification(
                    tenantEmail, ctx.period(), status,
                    summary.successfulInvoices(), summary.totalInvoices(), ctx.runId());
        }
        notificationService.sendErrorNotification(
                "Invoicing issue – seller: " + ctx.seller().getName()
                        + " [runId=" + ctx.runId() + "]",
                "Period: " + ctx.period() + "\nStatus: " + status,
                null);
    }
}