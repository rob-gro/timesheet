package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.exception.EmailException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.IntegrationException;
import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.tracking.EmailTrackingService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

import static dev.robgro.timesheet.invoice.EmailMessageService.COPY_EMAIL;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceDocumentServiceImpl implements InvoiceDocumentService {

    private final InvoiceRepository invoiceRepository;
    private final FtpService ftpService;
    private final PdfGenerator pdfGenerator;
    private final EmailMessageService emailMessageService;
    private final InvoiceSeller seller;
    private final EmailTrackingService trackingService;

    @Override
    @Transactional(readOnly = true)
    public byte[] getInvoicePdfContent(Long invoiceId) {
        Invoice invoice = getInvoiceOrThrow(invoiceId);
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
    public void savePdfAndSendInvoice(Long invoiceId) {
        log.info(" 😁 Processing invoice PDF generation and email for invoice id: {}", invoiceId);
        Invoice invoice = getInvoiceOrThrow(invoiceId);
        Client client = invoice.getClient();
        String fileName = invoice.getInvoiceNumber() + ".pdf";

        ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
        pdfGenerator.generateInvoicePdf(invoice, seller, pdfOutput);
        byte[] pdfContent = pdfOutput.toByteArray();

        ftpService.uploadPdfInvoice(fileName, pdfContent);

        invoice.setPdfPath(ftpService.getInvoicesDirectory() + "/" + fileName);
        invoice.setPdfGeneratedAt(LocalDateTime.now());

        // Create tracking token for email open tracking (90-day expiry)
        String trackingToken = trackingService.createTrackingToken(invoice);
        log.debug("Created email tracking token: {} for invoice: {}", trackingToken, invoiceId);

        try {
            String firstName = client.getClientName().split(" ")[0];
            String invoiceNumber = invoice.getInvoiceNumber();
            String preMonth = invoice.getIssueDate().getMonth().toString();
            String month = preMonth.charAt(0) + preMonth.substring(1).toLowerCase();

            InvoiceEmailRequest emailRequest = InvoiceEmailRequest.builder()
                    .recipientEmail(client.getEmail())
                    .ccEmail(COPY_EMAIL)
                    .firstName(firstName)
                    .invoiceNumber(invoiceNumber)
                    .month(month)
                    .fileName(fileName)
                    .attachment(pdfContent)
                    .numberOfVisits(invoice.getItemsList().size())
                    .totalAmount(invoice.getTotalAmount())
                    .trackingToken(trackingToken)  // Add tracking token
                    .build();

            emailMessageService.sendInvoiceEmail(emailRequest);

            invoice.setEmailSentAt(LocalDateTime.now());
            invoiceRepository.save(invoice);
            log.info("Successfully processed invoice id: {}", invoiceId);
        } catch (MessagingException e) {
            log.error("Failed to send invoice email for id: {}", invoiceId, e);
            throw new EmailException("Failed to send invoice email for invoice " + invoiceId, e);
        }
    }

    private Invoice getInvoiceOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", id));
    }
}
