package dev.robgro.timesheet.service;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.model.dto.*;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.model.entity.InvoiceItem;
import dev.robgro.timesheet.repository.ClientRepository;
import dev.robgro.timesheet.repository.InvoiceRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static dev.robgro.timesheet.service.EmailMessageService.COPY_EMAIL;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final TimesheetService timesheetService;
    private final InvoiceDtoMapper invoiceDtoMapper;
    private final ClientRepository clientRepository;
    private final InvoiceSeller seller;
    private final PdfGenerator pdfGenerator;
    private final EmailMessageService emailMessageService;
    private final FtpService ftpService;

    private final ClientDtoMapper clientDtoMapper;


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
        timesheets.forEach(timesheet -> timesheetService.markAsInvoiced(timesheet.id()));

        return invoiceDtoMapper.apply(savedInvoice);
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

    private String generateInvoiceNumber(LocalDate issueDate) {
        int year = issueDate.getYear();
        int month = issueDate.getMonthValue();
        String yearMonth = String.format("%02d-%d", month, year);
        long count = invoiceRepository.countByInvoiceNumberEndingWith(yearMonth);
        int nextNumber = (int) (count + 1);
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

        log.info("Processing invoice PDF generation and email for invoice id: {}", id);

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
    public List<InvoiceDto> searchInvoices(Long clientId, Integer year, Integer month, Boolean emailSent) {
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
                .filter(invoice -> clientId == null || invoice.getClient().getId().equals(clientId))
                .filter(invoice -> year == null || invoice.getIssueDate().getYear() == year)
                .filter(invoice -> month == null || invoice.getIssueDate().getMonthValue() == month)
                .filter(invoice -> emailSent == null || (emailSent && invoice.getEmailSentAt() != null)
                        || (!emailSent && invoice.getEmailSentAt() == null))
                .map(invoiceDtoMapper)
                .collect(toList());
    }

    @Transactional
    @Override
    public void deleteInvoice(Long id) {
        Invoice invoice = getInvoiceOrThrow(id);
        invoice.getItemsList().forEach(item ->
                timesheetService.updateInvoiceFlag(item.getTimesheetId(), false));
        invoice.getItemsList().clear();
        invoiceRepository.saveAndFlush(invoice);
        invoiceRepository.delete(invoice);
    }
}
