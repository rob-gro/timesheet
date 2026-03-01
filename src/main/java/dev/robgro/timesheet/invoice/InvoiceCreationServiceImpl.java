package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientService;
import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import dev.robgro.timesheet.timesheet.TimesheetDto;
import dev.robgro.timesheet.timesheet.Timesheet;
import dev.robgro.timesheet.client.ClientRepository;
import dev.robgro.timesheet.timesheet.TimesheetRepository;
import dev.robgro.timesheet.timesheet.TimesheetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service("dedicatedInvoiceCreationService")
@RequiredArgsConstructor
public class    InvoiceCreationServiceImpl implements InvoiceCreationService {

    private final ClientService clientService;
    private final TimesheetService timesheetService;
    private final InvoiceRepository invoiceRepository;
    private final TimesheetRepository timesheetRepository;
    private final InvoiceDtoMapper invoiceDtoMapper;
    private final ClientRepository clientRepository;
    private final SellerRepository sellerRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Transactional
    @Override
    public InvoiceDto createInvoiceFromTimesheets(ClientDto client, Seller seller, List<TimesheetDto> timesheets, LocalDate issueDate) {
        Invoice invoice = new Invoice();
        invoice.setClient(clientRepository.getReferenceById(client.id()));
        invoice.setSeller(seller);
        invoice.setIssueDate(issueDate);

        // Generate invoice number using new configurable system
        GeneratedInvoiceNumber generatedNumber = invoiceNumberGenerator.generateInvoiceNumber(seller.getId(), issueDate, null);
        invoice.setInvoiceNumberComponents(
            generatedNumber.getSequenceNumber(),
            generatedNumber.getPeriodYear(),
            generatedNumber.getPeriodMonth(),
            generatedNumber.getDisplayNumber(),
            generatedNumber.getSchemeId()
        );

        List<InvoiceItem> items = timesheets.stream()
                .map(timesheet -> createInvoiceItem(timesheet, invoice))
                .collect(Collectors.toList());

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
                .collect(Collectors.toList());

        timesheetRepository.saveAll(updatedTimesheets);

        Invoice refreshedInvoice = invoiceRepository.findById(savedInvoice.getId()).orElseThrow();
        return invoiceDtoMapper.apply(refreshedInvoice);
    }

    @Transactional
    public InvoiceDto createInvoice(Long clientId, Long sellerId, LocalDate issueDate, List<Long> timesheetIds) {
        if (timesheetIds.isEmpty()) {
            throw new ValidationException("No timesheets selected for invoice");
        }

        ClientDto client = clientService.getClientById(clientId);

        // Validate and fetch seller
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ValidationException("Seller not found with ID: " + sellerId));

        if (!seller.isActive()) {
            throw new BusinessRuleViolationException("Cannot create invoice: seller is inactive");
        }

        List<TimesheetDto> selectedTimesheets = timesheetIds.stream()
                .map(timesheetService::getTimesheetById)
                .filter(timesheet -> !timesheet.invoiced())
                .collect(Collectors.toList());

        if (selectedTimesheets.isEmpty()) {
            throw new BusinessRuleViolationException("All selected timesheets are already invoiced");
        }

        // Validate that all timesheets belong to the selected client
        boolean allBelongToClient = selectedTimesheets.stream()
                .allMatch(timesheet -> timesheet.clientId().equals(clientId));

        if (!allBelongToClient) {
            throw new BusinessRuleViolationException("Cannot create invoice: selected timesheets belong to different clients");
        }

        return createInvoiceFromTimesheets(client, seller, selectedTimesheets, issueDate);
    }

    private InvoiceItem createInvoiceItem(TimesheetDto timesheet, Invoice invoice) {
        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setServiceDate(timesheet.serviceDate());

        // HOTFIX: Use seller's service description + space + date
        String serviceDesc = invoice.getSeller() != null && invoice.getSeller().getServiceDescription() != null
            ? invoice.getSeller().getServiceDescription()
            : "Service";
        item.setDescription(serviceDesc + " " + timesheet.serviceDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

        item.setAmount(calculateAmount(timesheet.duration(), timesheet.hourlyRate()));
        item.setDuration(timesheet.duration());
        item.setHourlyRate(timesheet.hourlyRate());
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

    @Override
    public InvoiceDto buildInvoicePreview(Long clientId, Long sellerId, LocalDate issueDate, List<Long> timesheetIds) {
        if (timesheetIds.isEmpty()) {
            throw new ValidationException("No timesheets selected for invoice");
        }

        ClientDto client = clientService.getClientById(clientId);

        // Validate and fetch seller
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new ValidationException("Seller not found with ID: " + sellerId));

        if (!seller.isActive()) {
            throw new BusinessRuleViolationException("Cannot create invoice: seller is inactive");
        }

        List<TimesheetDto> selectedTimesheets = timesheetIds.stream()
                .map(timesheetService::getTimesheetById)
                .filter(timesheet -> !timesheet.invoiced())
                .collect(Collectors.toList());

        if (selectedTimesheets.isEmpty()) {
            throw new BusinessRuleViolationException("All selected timesheets are already invoiced");
        }

        // Validate that all timesheets belong to the selected client
        boolean allBelongToClient = selectedTimesheets.stream()
                .allMatch(timesheet -> timesheet.clientId().equals(clientId));

        if (!allBelongToClient) {
            throw new BusinessRuleViolationException("Cannot create invoice: selected timesheets belong to different clients");
        }

        // HOTFIX: Peek invoice number for preview WITHOUT reserving it
        // CRITICAL: Must use peekNextInvoiceNumber() not generateInvoiceNumber()
        // to avoid incrementing counter twice (preview + actual creation)
        GeneratedInvoiceNumber generatedNumber = invoiceNumberGenerator.peekNextInvoiceNumber(sellerId, issueDate, null);
        String invoiceNumber = generatedNumber.getDisplayNumber();

        // Build invoice items without persisting
        // HOTFIX: Use seller's service description + space + date
        String serviceDesc = seller != null && seller.getServiceDescription() != null
            ? seller.getServiceDescription()
            : "Service";

        List<InvoiceItemDto> items = selectedTimesheets.stream()
                .map(timesheet -> new InvoiceItemDto(
                        null, // no ID for preview
                        timesheet.serviceDate(),
                        serviceDesc + " " + timesheet.serviceDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        timesheet.duration(),
                        calculateAmount(timesheet.duration(), timesheet.hourlyRate()),
                        timesheet.hourlyRate()
                ))
                .collect(Collectors.toList());

        BigDecimal totalAmount = items.stream()
                .map(InvoiceItemDto::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Return preview InvoiceDto without saving to database
        return new InvoiceDto(
                null, // no ID for preview
                clientId,
                client.clientName(),
                sellerId,
                seller.getName(),
                invoiceNumber,
                issueDate,
                totalAmount,
                null, // no PDF path yet
                items,
                null, // no PDF generated timestamp yet
                null, // no email sent timestamp yet
                null, // no email opened timestamp yet
                0,    // no email open count yet
                null, // no last email opened timestamp yet
                "NOT_SENT" // email status
        );
    }
}
