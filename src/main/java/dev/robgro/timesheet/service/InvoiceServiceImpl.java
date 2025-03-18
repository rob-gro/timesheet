package dev.robgro.timesheet.service;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.*;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import dev.robgro.timesheet.model.entity.Timesheet;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.InvoiceRepository;
import dev.robgro.timesheet.repository.TimesheetRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.robgro.timesheet.service.EmailMessageService.COPY_EMAIL;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    private final InvoiceRepository invoiceRepository;
    private final TimesheetRepository timesheetRepository;
    private final InvoiceDtoMapper invoiceDtoMapper;
    private final ClientRepository clientRepository;
    private final InvoiceSeller seller;
    private final PdfGenerator pdfGenerator;
    private final EmailMessageService emailMessageService;
    private final FtpService ftpService;
    private final TimesheetService timesheetService;

    @Qualifier("dedicatedInvoiceCreationService")
    private final InvoiceCreationService invoiceCreationService;
//    private final BillingService billingService;

    @Override
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public List<InvoiceDto> getAllInvoicesOrderByDateDesc() {
        return invoiceRepository.findAllByOrderByIssueDateDesc()
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public List<InvoiceDto> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByIssueDateBetweenOrderByIssueDateDesc(startDate, endDate)
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    public InvoiceDto getInvoiceById(long id) {
        return invoiceDtoMapper.apply(getInvoiceOrThrow(id));
    }

    @Override
    public Optional<InvoiceDto> findByInvoiceNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .map(invoiceDtoMapper);
    }

    @Override
    public List<InvoiceDto> searchAndSortInvoices(Long clientId, Integer year, Integer month, String sortBy, String sortDir) {
        List<InvoiceDto> invoices = searchInvoices(clientId, year, month);
        return sortInvoices(invoices, sortBy, sortDir);
    }

    private List<InvoiceDto> sortInvoices(List<InvoiceDto> invoices, String sortBy, String sortDir) {
        Comparator<InvoiceDto> comparator = switch (sortBy) {
            case "invoiceNumber" -> Comparator
                    .comparing((InvoiceDto i) -> i.invoiceNumber().substring(i.invoiceNumber().length() - 4))
                    .thenComparing(i -> i.invoiceNumber().substring(4, 6))
                    .thenComparing(i -> i.invoiceNumber().substring(0, 3));
            case "issueDate" -> Comparator.comparing(InvoiceDto::issueDate);
            case "clientName" -> Comparator.comparing(InvoiceDto::clientName);
            case "totalAmount" -> Comparator.comparing(InvoiceDto::totalAmount);
            default -> Comparator.comparing(InvoiceDto::invoiceNumber);
        };
        return invoices.stream()
                .sorted(sortDir.equals("desc") ? comparator.reversed() : comparator)
                .collect(Collectors.toList());
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
    public byte[] getInvoicePdfContent(Long invoiceId) {
        Invoice invoice = getInvoiceOrThrow(invoiceId);
        if (invoice.getPdfPath() == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "PDF not found for invoice: " + invoiceId);
        }
        try {
            String fileName = invoice.getInvoiceNumber() + ".pdf";
            return ftpService.downloadPdfInvoice(fileName);
        } catch (Exception e) {
            log.error("Error downloading PDF for invoice: {}", invoiceId, e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Could not download PDF");
        }
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

    @Override
    public InvoiceDto createAndRedirectInvoice(CreateInvoiceRequest request) {
        return invoiceCreationService.createInvoice(
                request.clientId(),
                request.issueDate(),
                request.timesheetIds()
        );
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

    @Transactional
    @Override
    public void savePdfAndSendInvoice(Long id) {
        log.info(" 😁 Processing invoice PDF generation and email for invoice id: {}", id);
        Invoice invoice = getInvoiceOrThrow(id);
        Client client = invoice.getClient();
        String fileName = invoice.getInvoiceNumber() + ".pdf";

        ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
        pdfGenerator.generateInvoicePdf(invoice, seller, pdfOutput);
        byte[] pdfContent = pdfOutput.toByteArray();

        ftpService.uploadPdfInvoice(fileName, pdfContent);

        invoice.setPdfPath(ftpService.getInvoicesDirectory() + "/" + fileName);
        invoice.setPdfGeneratedAt(LocalDateTime.now());

        try {
            String firstName = client.getClientName().split(" ")[0];
            String invoiceNumber = invoice.getInvoiceNumber();
            String preMonth = invoice.getIssueDate().getMonth().toString();
            String month = preMonth.charAt(0) + preMonth.substring(1).toLowerCase();

            emailMessageService.sendInvoiceEmailWithBytes(
                    client.getEmail(),
                    COPY_EMAIL,
                    firstName,
                    invoiceNumber,
                    month,
                    fileName,
                    pdfContent
            );

            invoice.setEmailSentAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Successfully processed invoice id: {}", id);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email for id: {}", id, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public InvoiceReportData generateReport(DateRangeRequest dateRange, Long clientId) {
        List<InvoiceDto> invoices = searchInvoices(dateRange, clientId, null).getContent();

        List<InvoiceDto> sortedInvoices = invoices.stream()
                .sorted(Comparator.comparing(InvoiceDto::issueDate))
                .toList();

        BigDecimal totalAmount = invoices.stream()
                .map(InvoiceDto::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = generatePeriodLabel(dateRange);

        String clientName = null;
        if (clientId != null) {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client != null) {
                clientName = client.getClientName();
            }
        }

        return new InvoiceReportData(sortedInvoices, totalAmount, period, clientName);
    }

    @Transactional
    @Override
    public InvoiceDto updateInvoice(Long id, InvoiceUpdateRequest request) {
        Invoice invoice = getInvoiceOrThrow(id);
        Client newClient = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Client not found: " + request.clientId()));

        if (!invoice.getInvoiceNumber().equals(request.invoiceNumber())) {
            invoiceRepository.findByInvoiceNumber(request.invoiceNumber())
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Invoice number already exists: " + request.invoiceNumber());
                    });
        }

        List<Timesheet> oldTimesheets = new ArrayList<>(invoice.getTimesheets());

        invoice.setIssueDate(request.issueDate());
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setClient(newClient);

        oldTimesheets.forEach(timesheet -> timesheetService.detachFromInvoice(timesheet.getId()));

        invoiceRepository.save(invoice);

        invoiceRepository.deleteInvoiceItemsByInvoiceId(id);

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InvoiceItemUpdateRequest itemRequest : request.items()) {
            totalAmount = totalAmount.add(itemRequest.amount());

            InvoiceItem item = new InvoiceItem();
            item.setInvoice(invoice);
            item.setServiceDate(itemRequest.serviceDate());
            item.setDescription(itemRequest.description());
            item.setDuration(itemRequest.duration());
            item.setAmount(itemRequest.amount());

            if (itemRequest.timesheetId() != null) {
                item.setTimesheetId(itemRequest.timesheetId());
                Timesheet timesheet = timesheetRepository.findById(itemRequest.timesheetId())
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Timesheet not found: " + itemRequest.timesheetId()));

                timesheet.setInvoiced(true);
                timesheet.setInvoice(invoice);
                timesheet.setInvoiceNumber(invoice.getInvoiceNumber());
                timesheetRepository.save(timesheet);
            }
            invoice.getItemsList().add(item);
        }
        invoice.setTotalAmount(totalAmount);

        if (invoice.getPdfPath() != null) {
            invoice.setPdfGeneratedAt(null);
            invoice.setPdfPath(null);
        }
        return invoiceDtoMapper.apply(invoiceRepository.save(invoice));
    }

    @Override
    public List<InvoiceDto> searchInvoices(Long clientId, Integer year, Integer month) {
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(invoice -> clientId == null || invoice.getClient().getId().equals(clientId))
                .filter(invoice -> year == null || invoice.getIssueDate().getYear() == year)
                .filter(invoice -> month == null || invoice.getIssueDate().getMonthValue() == month)
                .map(invoiceDtoMapper)
                .collect(toList());
    }

    @Transactional
    @Override
    public void deleteInvoice(Long id, boolean deleteTimesheets, boolean detachFromClient) {
        log.info("Starting deletion of invoice ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found with id: " + id));

        log.info("Found invoice: {}, associated timesheets: {}",
                invoice.getInvoiceNumber(), invoice.getTimesheets().size());

        List<Timesheet> timesheetsToProcess = new ArrayList<>(invoice.getTimesheets());
        for (Timesheet timesheet : timesheetsToProcess) {
            log.info("Processing timesheet ID: {}", timesheet.getId());
            timesheet.setInvoice(null);
            timesheet.setInvoiced(false);

            if (deleteTimesheets) {
                log.info("Deleting timesheet ID: {}", timesheet.getId());
                invoiceRepository.deleteInvoiceItemsByInvoiceId(id);
            } else {
                log.info("Preserving timesheet ID: {}", timesheet.getId());
                timesheetRepository.save(timesheet);
            }
        }

        log.info("Deleting invoice items");
        jdbcTemplate.update("DELETE FROM invoice_items WHERE invoice_id = ?", id);

        log.info("Deleting invoice");
        invoiceRepository.delete(invoice);

        log.info("Successfully deleted invoice ID: {}", id);
    }

    @Override
    public Page<InvoiceDto> getAllInvoicesPageable(Long clientId, Integer year, Integer month, Pageable pageable) {
        return invoiceRepository.findFilteredInvoices(clientId, year, month, pageable)
                .map(invoiceDtoMapper);
    }

    @Override
    public Page<InvoiceDto> searchInvoices(DateRangeRequest dateRange, Long clientId, Pageable pageable) {
        LocalDate fromDate = convertToStartDate(dateRange);
        LocalDate toDate = convertToEndDate(dateRange);

        validateDateRange(fromDate, toDate);

        return invoiceRepository.findByDateRangeAndClient(fromDate, toDate, clientId, pageable)
                .map(invoiceDtoMapper);
    }

    private LocalDate convertToStartDate(DateRangeRequest range) {
        return range.fromYear() != null && range.fromMonth() != null
                ? LocalDate.of(range.fromYear(), range.fromMonth(), 1)
                : null;
    }

    private LocalDate convertToEndDate(DateRangeRequest range) {
        return range.toYear() != null && range.toMonth() != null
                ? LocalDate.of(range.toYear(), range.toMonth(), 1).plusMonths(1).minusDays(1)
                : null;
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return;
        }

        if (fromDate.isAfter(toDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date cannot be after end date");
        }
    }

    private String generatePeriodLabel(DateRangeRequest dateRange) {
        Integer fromYear = dateRange.fromYear();
        Integer fromMonth = dateRange.fromMonth();
        Integer toYear = dateRange.toYear();
        Integer toMonth = dateRange.toMonth();

        if (fromYear == null || fromMonth == null || toYear == null || toMonth == null) {
            return "all dates";
        }

        return Month.of(fromMonth) + " " + fromYear + " - " + Month.of(toMonth) + " " + toYear;
    }
}
