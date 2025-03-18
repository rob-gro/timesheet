package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.InvoiceDtoMapper;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import dev.robgro.timesheet.model.entity.Timesheet;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.InvoiceRepository;
import dev.robgro.timesheet.repository.TimesheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service("dedicatedInvoiceCreationService")
@RequiredArgsConstructor
public class InvoiceCreationServiceImpl implements InvoiceCreationService {

    private final ClientService clientService;
    private final TimesheetService timesheetService;
    private final InvoiceRepository invoiceRepository;
    private final TimesheetRepository timesheetRepository;
    private final InvoiceDtoMapper invoiceDtoMapper;
    private final ClientRepository clientRepository;

    @Transactional
    @Override
    public InvoiceDto createInvoiceFromTimesheets(ClientDto client, List<TimesheetDto> timesheets, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(clientRepository.getReferenceById(client.id()));
        invoice.setIssueDate(issueDate);
        invoice.setInvoiceNumber(generateInvoiceNumber(issueDate));

        List<InvoiceItem> items = timesheets.stream()
                .map(timesheet -> createInvoiceItem(timesheet, invoice))
                .toList();

        invoice.setItemsList(items);
        invoice.setTotalAmount(calculateTotalAmount(items));
        invoice.setIssuedDate(LocalDateTime.now());

        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<Timesheet> updatedTimesheets = timesheets.stream()
                .map(timesheet -> {
                    Timesheet ts = timesheetRepository.findById(timesheet.id()).orElseThrow();
                    ts.setInvoiced(true);
                    ts.setInvoice(savedInvoice);
                    ts.setInvoiceNumber(savedInvoice.getInvoiceNumber());
                    return ts;
                })
                .toList();

        timesheetRepository.saveAll(updatedTimesheets);

        Invoice refreshedInvoice = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        return invoiceDtoMapper.apply(refreshedInvoice);
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
                .filter(timesheet -> !timesheet.invoiced())
                .toList();

        if (selectedTimesheets.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "All selected timesheets are already invoiced");
        }

        return createInvoiceFromTimesheets(client, selectedTimesheets, issueDate);
    }

    private String generateInvoiceNumber(LocalDate issueDate) {
        int year = issueDate.getYear();
        int month = issueDate.getMonthValue();
        String yearMonth = String.format("%02d-%d", month, year);

        List<Integer> existingNumbers = invoiceRepository.findByInvoiceNumberEndingWith(yearMonth)
                .stream()
                .map(invoice -> Integer.parseInt(invoice.getInvoiceNumber().substring(0, 3)))
                .sorted()
                .toList();

        int nextNumber = 1;
        for (Integer existingNumber : existingNumbers) {
            if (existingNumber != nextNumber) {
                break;
            }
            nextNumber++;
        }
        return String.format("%03d-%s", nextNumber, yearMonth);
    }

    private InvoiceItem createInvoiceItem(TimesheetDto timesheet, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setServiceDate(timesheet.serviceDate());
        item.setDescription(String.format("Cleaning service - %s",
                timesheet.serviceDate().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        item.setAmount(calculateAmount(timesheet.duration(), invoice.getClient().getHourlyRate()));
        item.setDuration(timesheet.duration());
        item.setTimesheetId(timesheet.id());
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
}
