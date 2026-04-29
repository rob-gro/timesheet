package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.EmailException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.IntegrationException;
import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.tracking.EmailTrackingService;
import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static dev.robgro.timesheet.invoice.EmailMessageService.COPY_EMAIL;

@Service
@Slf4j
public class InvoiceDocumentServiceImpl implements InvoiceDocumentService {

    private final InvoiceRepository invoiceRepository;
    private final FtpService ftpService;
    private final PdfGenerator pdfGenerator;
    private final EmailMessageService emailMessageService;
    private final EmailTrackingService trackingService;
    private final TransactionTemplate tx;

    public InvoiceDocumentServiceImpl(
            InvoiceRepository invoiceRepository,
            FtpService ftpService,
            PdfGenerator pdfGenerator,
            EmailMessageService emailMessageService,
            EmailTrackingService trackingService,
            PlatformTransactionManager transactionManager) {
        this.invoiceRepository = invoiceRepository;
        this.ftpService = ftpService;
        this.pdfGenerator = pdfGenerator;
        this.emailMessageService = emailMessageService;
        this.trackingService = trackingService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getInvoicePdfContent(Long invoiceId) {
        Invoice invoice = getInvoiceOrThrow(invoiceId);
        if (invoice.getPdfPath() == null) {
            throw new EntityNotFoundException("PDF for invoice", invoiceId);
        }
        try {
            String fileName = sanitizeFilename(invoice.getInvoiceNumber()) + ".pdf";
            return ftpService.downloadPdfInvoice(fileName);
        } catch (Exception e) {
            log.error("Error downloading PDF for invoice: {}", invoiceId, e);
            throw new IntegrationException("Could not download PDF for invoice " + invoiceId, e);
        }
    }

    @Override
    public void savePdfAndSendInvoice(Long invoiceId, PrintMode printMode) {
        log.info("Processing invoice PDF generation and email for invoice id: {}", invoiceId);

        // Phase 1 (short TX): load invoice+client, generate PDF, FTP upload,
        // save pdfPath + pdfGeneratedAt + tracking token — COMMITS HERE
        // All lazy-loaded collections accessed while session is open.
        InvoiceEmailRequest emailRequest = tx.execute(status -> {
            Invoice invoice = getInvoiceOrThrow(invoiceId);
            Client client = invoice.getClient();
            String fileName = sanitizeFilename(invoice.getInvoiceNumber()) + ".pdf";

            ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
            pdfGenerator.generateInvoicePdf(invoice, pdfOutput, printMode);
            byte[] pdfContent = pdfOutput.toByteArray();

            ftpService.uploadPdfInvoice(fileName, pdfContent);

            invoice.setPdfPath(ftpService.getInvoicesDirectory() + "/" + fileName);
            invoice.setPdfGeneratedAt(LocalDateTime.now());
            invoiceRepository.save(invoice);

            String trackingToken = trackingService.createTrackingToken(invoice);
            log.debug("Created email tracking token: {} for invoice: {}", trackingToken, invoiceId);

            // Capture all email data while session is still open (lazy-load safe)
            String firstName = client.getClientName().split(" ")[0];
            String invoiceNumber = invoice.getInvoiceNumber();
            String preMonth = invoice.getIssueDate().getMonth().toString();
            String month = preMonth.charAt(0) + preMonth.substring(1).toLowerCase();
            int numberOfVisits = invoice.getItemsList().size();
            BigDecimal totalAmount = invoice.getTotalAmount();

            return InvoiceEmailRequest.builder()
                    .recipientEmail(client.getEmail())
                    .ccEmail(COPY_EMAIL)
                    .firstName(firstName)
                    .invoiceNumber(invoiceNumber)
                    .month(month)
                    .fileName(fileName)
                    .attachment(pdfContent)
                    .numberOfVisits(numberOfVisits)
                    .totalAmount(totalAmount)
                    .trackingToken(trackingToken)
                    .build();
        });

        // Phase 2 (no TX): SMTP — DB connection NOT held during email sending
        try {
            emailMessageService.sendInvoiceEmail(emailRequest);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email for id: {}", invoiceId, e);
            throw new EmailException("Failed to send invoice email for invoice " + invoiceId, e);
        }

        // Phase 3 (short TX): save emailSentAt — COMMITS HERE
        tx.executeWithoutResult(status -> {
            Invoice invoice = getInvoiceOrThrow(invoiceId);
            invoice.setEmailSentAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Successfully processed invoice id: {}", invoiceId);
        });
    }

    private Invoice getInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", id));
    }

    /**
     * HOTFIX: Sanitize invoice number for safe filename usage.
     * Replaces "/" with "-" to prevent treating invoice number as folder path.
     * Example: "INV/2026/001" → "INV-2026-001.pdf"
     */
    private String sanitizeFilename(String invoiceNumber) {
        return invoiceNumber.replace("/", "-");
    }
}
