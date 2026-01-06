package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.TimesheetService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
public class BillingServiceImpl implements BillingService {

    public BillingServiceImpl(ClientService clientService, InvoiceService invoiceService, InvoiceCreationService invoiceCreationService, TimesheetService timesheetService, SellerRepository sellerRepository) {
        this.clientService = clientService;
        this.invoiceService = invoiceService;
        this.invoiceCreationService = invoiceCreationService;
        this.timesheetService = timesheetService;
        this.sellerRepository = sellerRepository;
    }

    private final ClientService clientService;
    private final InvoiceService invoiceService;

    @Qualifier("dedicatedInvoiceCreationService")
    private final InvoiceCreationService invoiceCreationService;
    private final TimesheetService timesheetService;
    private final SellerRepository sellerRepository;

    public List<InvoiceDto> generateMonthlyInvoices(int year, int month) {
        List<ClientDto> clients = clientService.getAllClients();
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();

        return clients.stream()
                .map(client -> generateMonthlyInvoiceForClient(client.id(), year, month, lastDayOfMonth))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public InvoiceDto createMonthlyInvoice(Long clientId, int year, int month) {
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();
        return generateMonthlyInvoiceForClient(clientId, year, month, lastDayOfMonth)
                .orElseThrow(() -> new BusinessRuleViolationException("No uninvoiced timesheets found for this period"));
    }

    private Optional<InvoiceDto> generateMonthlyInvoiceForClient(Long clientId, int year, int month, LocalDate issueDate) {
        List<TimesheetDto> uninvoicedTimesheets = timesheetService.getMonthlyTimesheets(clientId, year, month)
                .stream()
                .filter(timesheet -> !timesheet.invoiced())
                .toList();

        if (uninvoicedTimesheets.isEmpty()) {
            return Optional.empty();
        }

        List<Long> timesheetIds = uninvoicedTimesheets.stream()
                .map(TimesheetDto::id)
                .toList();

        return Optional.of(createInvoice(clientId, issueDate, timesheetIds));
    }

    @Transactional
    public InvoiceDto createInvoice(Long clientId, LocalDate issueDate, List<Long> timesheetIds) {
        // Use system default seller for CRON-generated invoices
        Seller systemDefaultSeller = sellerRepository.findByIsSystemDefaultTrue()
                .orElseGet(() -> sellerRepository.findByActiveTrue().stream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessRuleViolationException("No active seller found. Please create an active seller first.")));

        return invoiceCreationService.createInvoice(clientId, systemDefaultSeller.getId(), issueDate, timesheetIds);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month) {
        return invoiceService.getMonthlyInvoices(clientId, year, month);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getYearlyInvoices(Long clientId, int year) {
        return invoiceService.getYearlyInvoices(clientId, year);
    }
}
