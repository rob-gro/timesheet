package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.client.ClientRepository;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.IntegrationException;
import dev.robgro.timesheet.exception.ResourceAlreadyExistsException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.timesheet.Timesheet;
import dev.robgro.timesheet.timesheet.TimesheetRepository;
import dev.robgro.timesheet.timesheet.TimesheetService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private final FtpService ftpService;
    private final TimesheetService timesheetService;
    private final InvoiceDocumentService invoiceDocumentService;

    @Qualifier("dedicatedInvoiceCreationService")
    private final InvoiceCreationService invoiceCreationService;


    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDto> getAllInvoices() {
        return invoiceRepository.findAll().stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDto> getAllInvoicesOrderByDateDesc() {
        return invoiceRepository.findAllByOrderByIssueDateDesc()
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceDto> getInvoicesByDateRange(LocalDate startDate, LocalDate endDate) {
        return invoiceRepository.findByIssueDateBetweenOrderByIssueDateDesc(startDate, endDate)
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceDto getInvoiceById(long id) {
        return invoiceDtoMapper.apply(getInvoiceOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public byte[] getInvoicePdfContent(Long invoiceId) {

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));

        if (invoice.getPdfPath() == null) {
            throw new EntityNotFoundException("PDF for invoice", invoiceId);
        }

        try {
            String fileName = invoice.getInvoiceNumber() + ".pdf";
            return ftpService.downloadPdfInvoice(fileName);
        } catch (Exception e) {
            log.error("Error downloading PDF for invoice: {}", invoiceId, e);
            throw new IntegrationException("Could not download PDF for invoice " + invoiceId, e);
        }
    }

    @Transactional
    @Override
    public void savePdfAndSendInvoice(Long id) {
        invoiceDocumentService.savePdfAndSendInvoice(id);
    }

    @Override
    @Transactional(readOnly = true)
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
    @Transactional(readOnly = true)
    public List<InvoiceDto> getYearlyInvoices(Long clientId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        return invoiceRepository.findByClientIdAndIssueDateBetween(clientId, startDate, endDate)
                .stream()
                .map(invoiceDtoMapper)
                .toList();
    }


    @Override
    public InvoiceDto createAndRedirectInvoice(CreateInvoiceRequest request) {
        return invoiceCreationService.createInvoice(
                request.clientId(),
                request.issueDate(),
                request.timesheetIds()
        );
    }

    private Invoice getInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", id));
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceReportData generateReport(DateRangeRequest dateRange, Long clientId) {
        LocalDate fromDate = convertToStartDate(dateRange);
        LocalDate toDate = convertToEndDate(dateRange);

        Sort sort = Sort.by(Sort.Direction.ASC, "issueDate");
        List<Invoice> invoices = invoiceRepository.findForReporting(clientId, fromDate, toDate, sort);

        List<InvoiceDto> sortedInvoiceDtos = invoices.stream()
                .map(invoiceDtoMapper)
                .toList();

        BigDecimal totalAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String period = generatePeriodLabel(dateRange);

        String clientName = null;
        if (clientId != null) {
            Client client = clientRepository.findById(clientId).orElse(null);
            if (client != null) {
                clientName = client.getClientName();
            }
        }

        return new InvoiceReportData(sortedInvoiceDtos, totalAmount, period, clientName);
    }

    @Transactional
    @Override
    public InvoiceDto updateInvoice(Long id, InvoiceUpdateRequest request) {
        Invoice invoice = getInvoiceOrThrow(id);
        Client newClient = clientRepository.findById(request.clientId())
                .orElseThrow(() -> new EntityNotFoundException("Client", request.clientId()));

        if (!invoice.getInvoiceNumber().equals(request.invoiceNumber())) {
            invoiceRepository.findByInvoiceNumber(request.invoiceNumber())
                    .ifPresent(existing -> {
                        throw new ResourceAlreadyExistsException("Invoice", "number", request.invoiceNumber());
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
            item.setHourlyRate(itemRequest.hourlyRate());

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
        return invoiceRepository.findFilteredInvoices(clientId, year, month).stream()
                .map(invoiceDtoMapper)
                .collect(toList());
    }

    @Transactional
    @Override
    public void deleteInvoice(Long id, boolean deleteTimesheets, boolean detachFromClient) {
        log.info("Starting deletion of invoice ID: {}", id);

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", id));

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
            throw new ValidationException("Start date cannot be after end date");
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
