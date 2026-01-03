package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.config.InvoicingSchedulerProperties;
import dev.robgro.timesheet.invoice.BillingService;
import dev.robgro.timesheet.invoice.InvoiceDto;
import dev.robgro.timesheet.invoice.InvoiceService;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicingTaskServiceImpl implements InvoicingTaskService {

    private final BillingService billingService;
    private final InvoiceService invoiceService;
    private final ClientService clientService;
    private final TimesheetService timesheetService;
    private final AdminNotificationService notificationService;
    private final InvoicingSchedulerProperties properties;

    @Override
    @Transactional
    public InvoicingSummary executeMonthlyInvoicing() {
        log.info("=== STARTING AUTOMATED MONTHLY INVOICING ===");

        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        log.info("Generating invoices for: {}", previousMonth);

        List<InvoiceDto> createdInvoices = billingService.generateMonthlyInvoices(
                previousMonth.getYear(),
                previousMonth.getMonthValue()
        );
        log.info("Created {} invoices", createdInvoices.size());

        List<InvoiceProcessingResult> results = new ArrayList<>();
        for (InvoiceDto invoice : createdInvoices) {
            InvoiceProcessingResult result = processInvoice(invoice);
            results.add(result);
        }

        List<String> emptyClients = findActiveClientsWithoutTimesheets(previousMonth);

        InvoicingSummary summary = InvoicingSummary.builder()
                .executionTime(LocalDateTime.now())
                .previousMonth(previousMonth)
                .totalInvoices(createdInvoices.size())
                .successfulInvoices(countSuccessful(results))
                .failedInvoices(countFailed(results))
                .clientsWithoutTimesheets(emptyClients)
                .processingResults(results)
                .build();

        sendAdminNotifications(summary, emptyClients);

        log.info("=== COMPLETED INVOICING: {} success, {} failed ===",
                summary.successfulInvoices(), summary.failedInvoices());

        return summary;
    }

    private InvoiceProcessingResult processInvoice(InvoiceDto invoice) {
        try {
            log.info("Processing invoice: {}, client: {}",
                    invoice.invoiceNumber(), invoice.clientName());

            invoiceService.savePdfAndSendInvoice(invoice.id());

            log.info("Successfully processed invoice: {}", invoice.invoiceNumber());
            return InvoiceProcessingResult.success(invoice);

        } catch (Exception e) {
            log.error("Failed to process invoice {}: {}",
                    invoice.invoiceNumber(), e.getMessage(), e);

            notificationService.sendErrorNotification(
                    "Invoice Processing Error: " + invoice.invoiceNumber(),
                    buildErrorDetails(invoice, e),
                    e
            );

            return InvoiceProcessingResult.failure(invoice, e);
        }
    }

    private List<String> findActiveClientsWithoutTimesheets(YearMonth month) {
        log.debug("Checking for active clients without timesheets");

        List<ClientDto> activeClients = clientService.getAllClients().stream()
                .filter(ClientDto::active)
                .toList();

        List<String> emptyClients = new ArrayList<>();
        for (ClientDto client : activeClients) {
            List<TimesheetDto> timesheets = timesheetService.getMonthlyTimesheets(
                    client.id(),
                    month.getYear(),
                    month.getMonthValue()
            );

            if (timesheets.isEmpty()) {
                log.info("Active client {} has no timesheets for {}", client.clientName(), month);
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

    private String buildErrorDetails(InvoiceDto invoice, Exception e) {
        return String.format(
                "Invoice: %s\nClient: %s\nClient ID: %d\nError: %s",
                invoice.invoiceNumber(),
                invoice.clientName(),
                invoice.clientId(),
                e.getMessage()
        );
    }

    private int countSuccessful(List<InvoiceProcessingResult> results) {
        return (int) results.stream().filter(InvoiceProcessingResult::isSuccess).count();
    }

    private int countFailed(List<InvoiceProcessingResult> results) {
        return (int) results.stream().filter(r -> !r.isSuccess()).count();
    }
}
