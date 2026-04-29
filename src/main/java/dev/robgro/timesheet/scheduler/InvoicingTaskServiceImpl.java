package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import dev.robgro.timesheet.invoice.InvoiceCreationService;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.delivery.InvoiceDeliveryJobRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class InvoicingTaskServiceImpl implements InvoicingTaskService {

    private final InvoiceCreationService invoiceCreationService;
    private final InvoiceDeliveryJobRepository deliveryJobRepository;
    private final ClientService clientService;
    private final TimesheetService timesheetService;
    private final AdminNotificationService notificationService;
    private final InvoicingSchedulerProperties properties;

    public InvoicingTaskServiceImpl(
            @Qualifier("dedicatedInvoiceCreationService") InvoiceCreationService invoiceCreationService,
            InvoiceDeliveryJobRepository deliveryJobRepository,
            ClientService clientService,
            TimesheetService timesheetService,
            AdminNotificationService notificationService,
            InvoicingSchedulerProperties properties) {
        this.invoiceCreationService = invoiceCreationService;
        this.deliveryJobRepository = deliveryJobRepository;
        this.clientService = clientService;
        this.timesheetService = timesheetService;
        this.notificationService = notificationService;
        this.properties = properties;
    }

    @Override
    public InvoicingSummary executeMonthlyInvoicing(Long sellerId, YearMonth period, String runId) {
        log.info("=== STARTING AUTOMATED MONTHLY INVOICING for seller={}, period={}, runId={} ===",
                sellerId, period, runId);

        LocalDate issueDate = period.atEndOfMonth();
        List<ClientDto> activeClients = clientService.getAllClients().stream()
                .filter(ClientDto::active)
                .toList();

        log.info("Processing {} active clients for period={}", activeClients.size(), period);

        List<InvoiceDto> createdInvoices = new ArrayList<>();
        List<InvoiceProcessingResult> results = new ArrayList<>();

        for (ClientDto client : activeClients) {
            List<TimesheetDto> uninvoiced = timesheetService.getMonthlyTimesheets(
                            client.id(), period.getYear(), period.getMonthValue())
                    .stream()
                    .filter(t -> !t.invoiced())
                    .toList();

            if (uninvoiced.isEmpty()) {
                continue;
            }

            List<Long> timesheetIds = uninvoiced.stream().map(TimesheetDto::id).toList();
            try {
                InvoiceDto invoice = invoiceCreationService.createInvoice(
                        client.id(), sellerId, issueDate, timesheetIds);
                createdInvoices.add(invoice);
                log.info("Created invoice {} for client {}", invoice.invoiceNumber(), client.clientName());

                InvoiceProcessingResult result = assignToRunAndSuccess(invoice, runId);
                results.add(result);
            } catch (Exception e) {
                log.error("Failed to create invoice for client {}: {}",
                        client.clientName(), e.getMessage(), e);
                notificationService.sendErrorNotification(
                        "Invoice Creation Error: client " + client.clientName(),
                        buildErrorDetails(client, e), e);
                results.add(InvoiceProcessingResult.failure(null, e));
            }
        }

        List<String> emptyClients = findActiveClientsWithoutTimesheets(activeClients, period);

        InvoicingSummary summary = InvoicingSummary.builder()
                .executionTime(LocalDateTime.now())
                .previousMonth(period)
                .totalInvoices(createdInvoices.size())
                .successfulInvoices(countSuccessful(results))
                .failedInvoices(countFailed(results))
                .clientsWithoutTimesheets(emptyClients)
                .processingResults(results)
                .runId(runId)
                .build();

        sendAdminNotifications(summary, emptyClients);

        log.info("=== COMPLETED INVOICING for seller={}: {} success, {} failed ===",
                sellerId, summary.successfulInvoices(), summary.failedInvoices());

        return summary;
    }

    /**
     * Assigns the scheduler runId to the delivery job so run-status polling works.
     * Delivery itself is async — the worker picks up PENDING jobs independently.
     * If runId is null (manual/test trigger) or the job lookup fails, we still return
     * success — the delivery job was created by InvoiceCreationService and will be processed.
     */
    private InvoiceProcessingResult assignToRunAndSuccess(InvoiceDto invoice, String runId) {
        if (runId != null) {
            try {
                deliveryJobRepository.findByInvoice_IdAndIsActive(invoice.id(), true)
                        .ifPresent(job -> {
                            job.assignToRun(runId);
                            deliveryJobRepository.save(job);
                        });
            } catch (Exception e) {
                log.warn("Could not assign runId={} to delivery job for invoice {} — " +
                        "job will still be processed by worker: {}",
                        runId, invoice.invoiceNumber(), e.getMessage());
            }
        }
        log.info("Invoice {} queued for async delivery", invoice.invoiceNumber());
        return InvoiceProcessingResult.success(invoice);
    }

    private List<String> findActiveClientsWithoutTimesheets(List<ClientDto> activeClients, YearMonth period) {
        List<String> emptyClients = new ArrayList<>();
        for (ClientDto client : activeClients) {
            List<TimesheetDto> timesheets = timesheetService.getMonthlyTimesheets(
                    client.id(), period.getYear(), period.getMonthValue());
            if (timesheets.isEmpty()) {
                log.info("Active client {} has no timesheets for {}", client.clientName(), period);
                emptyClients.add(client.clientName());
            }
        }
        return emptyClients;
    }

    private void sendAdminNotifications(InvoicingSummary summary, List<String> emptyClients) {
        if (properties.isSendSummaryEmail()) {
            notificationService.sendSummaryNotification(summary);
        }
        if (properties.isSendEmptyClientWarning() && !emptyClients.isEmpty()) {
            notificationService.sendEmptyClientWarning(emptyClients);
        }
    }

    private String buildErrorDetails(ClientDto client, Exception e) {
        return String.format("Client: %s (id=%d)\nError: %s",
                client.clientName(), client.id(), e.getMessage());
    }

    private int countSuccessful(List<InvoiceProcessingResult> results) {
        return (int) results.stream().filter(InvoiceProcessingResult::isSuccess).count();
    }

    private int countFailed(List<InvoiceProcessingResult> results) {
        return (int) results.stream().filter(r -> !r.isSuccess()).count();
    }
}
