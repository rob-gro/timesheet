package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {
    private final ClientService clientService;
    private final InvoiceService invoiceService;
    private final TimesheetService timesheetService;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "No uninvoiced timesheets found for this period"));
    }

    private Optional<InvoiceDto> generateMonthlyInvoiceForClient(Long clientId, int year, int month, LocalDate issueDate) {
        List<TimesheetDto> uninvoicedTimesheets = timesheetService.getMonthlyTimesheets(clientId, year, month)
                .stream()
                .filter(timesheet -> !timesheet.isInvoice())
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
        if (timesheetIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No timesheets selected for invoice");
        }

        ClientDto client = clientService.getClientById(clientId);
        List<TimesheetDto> selectedTimesheets = timesheetIds.stream()
                .map(timesheetService::getTimesheetById)
                .filter(timesheet -> !timesheet.isInvoice())
                .toList();

        if (selectedTimesheets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "All selected timesheets are already invoiced");
        }

        return invoiceService.createInvoiceFromTimesheets(client, selectedTimesheets, issueDate);
    }

    public List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month) {
        return invoiceService.getMonthlyInvoices(clientId, year, month);
    }

    public List<InvoiceDto> getYearlyInvoices(Long clientId, int year) {
        return invoiceService.getYearlyInvoices(clientId, year);
    }
}
