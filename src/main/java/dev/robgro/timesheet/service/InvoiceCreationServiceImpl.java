package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.ValidationException;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Transactional
    @Override
    public InvoiceDto createInvoiceFromTimesheets(ClientDto client, List<TimesheetDto> timesheets, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(clientRepository.getReferenceById(client.id()));
        invoice.setIssueDate(issueDate);
        invoice.setInvoiceNumber(invoiceNumberGenerator.generateInvoiceNumber(issueDate));

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
            throw new ValidationException("No timesheets selected for invoice");
        }

        ClientDto client = clientService.getClientById(clientId);
        List<TimesheetDto> selectedTimesheets = timesheetIds.stream()
                .map(timesheetService::getTimesheetById)
                .filter(timesheet -> !timesheet.invoiced())
                .toList();

        if (selectedTimesheets.isEmpty()) {
            throw new BusinessRuleViolationException("All selected timesheets are already invoiced");
        }

        return createInvoiceFromTimesheets(client, selectedTimesheets, issueDate);
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
