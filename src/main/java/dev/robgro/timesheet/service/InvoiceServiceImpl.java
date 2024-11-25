package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.*;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientService clientService;
    private final TimesheetService timesheetService;
    private final InvoiceDtoMapper invoiceDtoMapper;
    private final ClientDtoMapper clientDtoMapper;
    private final TimesheetDtoMapper timesheetDtoMapper;
    private final ClientRepository clientRepository;

    @Override
    public List<InvoiceDto> generateMonthlyInvoices(int year, int month) {
        List<ClientDto> clients = clientService.getAllClients();

        return clients.stream()
                .map(client -> generateMonthlyInvoiceForClient(client.id(), year, month))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    public Optional<InvoiceDto> generateMonthlyInvoiceForClient(Long clientId, int year, int month) {
        List<TimesheetDto> uninvoicedTimesheets = timesheetService.getMonthlyTimesheets(clientId, year, month)
                .stream()
                .filter(timesheet -> !timesheet.isInvoice())
                .toList();

        if (uninvoicedTimesheets.isEmpty()) {
            return Optional.empty();
        }

        List<Long> timesheetsIds = uninvoicedTimesheets.stream()
                .map(TimesheetDto::id)
                .toList();
        return Optional.of(createInvoice(clientId, year, month, timesheetsIds));
    }

    @Override
    public InvoiceDto createInvoice(Long clientId, int year, int month, List<Long> timesheetsIds) {
        LocalDate lastDayOfMonth = YearMonth.of(year, month).atEndOfMonth();
        return createInvoice(clientId, lastDayOfMonth, timesheetsIds);
    }

    @Override
    public InvoiceDto createInvoice(Long clientId, LocalDate issueDate, List<Long> timesheetsIds) {
        if (timesheetsIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "No timesheets selected for invoice");
        }

        ClientDto clientDto = clientService.getClientById(clientId);
        List<TimesheetDto> selectedTimeshseets = timesheetsIds.stream()
                .map(timesheetService::getTimesheetById)
                .filter(timesheet -> timesheet.isInvoice())
                .toList();

        if (selectedTimeshseets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "All selected timesheets are alredy invoiced");
        }

        Invoice invoice = createInvoiceFromTimesheets(clientDto, selectedTimeshseets, issueDate);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        timesheetsIds.forEach(timesheetService::markAsInvoiced);

        return invoiceDtoMapper.apply(savedInvoice);
    }

    @Override
    public InvoiceDto createMonthlyInvoice(Long clientId, int year, int month) {
        Optional<InvoiceDto> invoice = generateMonthlyInvoiceForClient(clientId, year, month);
        return invoice.orElseThrow(() -> new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "No uninvoiced timesheets found"));
    }

    @Override
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public InvoiceDto getInvoiceById(long id) {
        return invoiceDtoMapper.apply(getInvoiceOrThrow(id));
    }

    @Override
    public List<InvoiceDto> getMonthlyInvoices(Long clientId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return invoiceRepository.findByClientIdAndIssueDateBetween(clientId, startDate, endDate)
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public List<InvoiceDto> getYearlyInvoices(Long clientId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return invoiceRepository.findByClientIdAndIssueDateBetween(clientId, startDate, endDate)
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public Optional<InvoiceDto> findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .map(invoiceDtoMapper);
    }

    private Invoice createInvoiceFromTimesheets(ClientDto clientDto, List<TimesheetDto> timesheets, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(clientRepository.getReferenceById(clientDto.id()));
        invoice.setIssueDate(issueDate);
        invoice.setInvoiceNumber(generateInvoiceNumber(issueDate));

        List<InvoiceItem> items = timesheets.stream()
                .map(timesheet -> createInvoiceItem(timesheet, invoice))
                .toList();

        invoice.setItemsList(items);
        invoice.setTotalAmount(calculateTotalAmount(items));

        return invoice;
    }

    private String generateInvoiceNumber(LocalDate issueDate) {
        int year = issueDate.getYear();
        int month = issueDate.getMonthValue();
        String yearMonth = String.format("%02d/%d", month, year);
        long count = invoiceRepository.countByInvoiceNumberEndingWith(yearMonth);
        int nextNumber = (int) (count + 1);

        return String.format("%03d/%s", nextNumber, yearMonth);
    }

    private InvoiceItem createInvoiceItem(TimesheetDto timesheet, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setServiceDate(timesheet.serviceDate());
        item.setDescription("Cleaning service");
        item.setAmount(calculateAmount(timesheet.duration(), invoice.getClient().getHourlyRate()));
        return item;
    }

    private BigDecimal calculateAmount(double duration, double hourlyRate) {
        return BigDecimal.valueOf(duration * hourlyRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTotalAmount(List<InvoiceItem> items) {
        return items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Invoice getInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invoice with id: " + id + "not found"));
    }
}
