package dev.robgro.timesheet.service;

import dev.robgro.timesheet.config.InvoiceSeller;
import dev.robgro.timesheet.exception.EmailException;
import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.IntegrationException;
import dev.robgro.timesheet.model.entity.Client;
import dev.robgro.timesheet.model.entity.Invoice;
import dev.robgro.timesheet.repository.InvoiceRepository;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;

import static dev.robgro.timesheet.service.EmailMessageService.COPY_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceDocumentServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private FtpService ftpService;

    @Mock
    private PdfGenerator pdfGenerator;

    @Mock
    private EmailMessageService emailMessageService;

    @Mock
    private InvoiceSeller seller;

    @InjectMocks
    private InvoiceDocumentServiceImpl invoiceDocumentService;

    // ----- PDF Content Retrieval -----

    @Test
    void shouldGetInvoicePdfContent() {
        // given
        Long invoiceId = 1L;
        String invoiceNumber = "001-01-2023";
        String pdfPath = "/path/to/pdf";
        String fileName = invoiceNumber + ".pdf";

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPdfPath(pdfPath);

        byte[] expectedPdfContent = "PDF content".getBytes();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.downloadPdfInvoice(fileName)).thenReturn(expectedPdfContent);

        // when
        byte[] result = invoiceDocumentService.getInvoicePdfContent(invoiceId);

        // then
        assertThat(result).isEqualTo(expectedPdfContent);
        verify(invoiceRepository).findById(invoiceId);
        verify(ftpService).downloadPdfInvoice(fileName);
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFoundDuringPdfRetrieval() {
        // given
        Long invoiceId = 1L;
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> invoiceDocumentService.getInvoicePdfContent(invoiceId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice with id 1 not found");

        verify(invoiceRepository).findById(invoiceId);
        verifyNoInteractions(ftpService);
    }

    @Test
    void shouldThrowExceptionWhenPdfPathIsNull() {
        // given
        Long invoiceId = 1L;
        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setPdfPath(null);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));

        // when/then
        assertThatThrownBy(() -> invoiceDocumentService.getInvoicePdfContent(invoiceId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("PDF for invoice with id 1 not found");

        verify(invoiceRepository).findById(invoiceId);
        verifyNoInteractions(ftpService);
    }

    @Test
    void shouldThrowExceptionWhenFtpDownloadFails() {
        // given
        Long invoiceId = 1L;
        String invoiceNumber = "001-01-2023";
        String pdfPath = "/path/to/pdf";
        String fileName = invoiceNumber + ".pdf";

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setPdfPath(pdfPath);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.downloadPdfInvoice(fileName)).thenThrow(new RuntimeException("FTP error"));

        // when/then
        assertThatThrownBy(() -> invoiceDocumentService.getInvoicePdfContent(invoiceId))
                .isInstanceOf(IntegrationException.class)
                .hasMessageContaining("Could not download PDF for invoice 1");

        verify(invoiceRepository).findById(invoiceId);
        verify(ftpService).downloadPdfInvoice(fileName);
    }

    // ----- PDF Generation and Email Sending -----

    @Test
    void shouldSavePdfAndSendInvoice() throws MessagingException {
        // given
        Long invoiceId = 1L;
        String invoiceNumber = "001-01-2023";
        LocalDate issueDate = LocalDate.of(2023, Month.JANUARY, 15);
        String clientName = "John Doe";
        String clientEmail = "john@example.com";
        String ftpDirectory = "/invoices";

        Client client = new Client();
        client.setClientName(clientName);
        client.setEmail(clientEmail);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        invoice.setClient(client);

        byte[] pdfContent = "PDF content".getBytes();
        ByteArrayOutputStream pdfOutput = new ByteArrayOutputStream();
        pdfOutput.write(pdfContent, 0, pdfContent.length);

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.getInvoicesDirectory()).thenReturn(ftpDirectory);
        doNothing().when(pdfGenerator).generateInvoicePdf(eq(invoice), eq(seller), any(ByteArrayOutputStream.class));
        doNothing().when(ftpService).uploadPdfInvoice(eq(invoiceNumber + ".pdf"), any(byte[].class));
        doNothing().when(emailMessageService).sendInvoiceEmailWithBytes(
                eq(clientEmail),
                eq(COPY_EMAIL),
                eq("John"),
                eq(invoiceNumber),
                eq("January"),
                eq(invoiceNumber + ".pdf"),
                any(byte[].class)
        );

        // when
        invoiceDocumentService.savePdfAndSendInvoice(invoiceId);

        // then
        verify(invoiceRepository).findById(invoiceId);
        verify(pdfGenerator).generateInvoicePdf(eq(invoice), eq(seller), any(ByteArrayOutputStream.class));
        verify(ftpService).uploadPdfInvoice(eq(invoiceNumber + ".pdf"), any(byte[].class));
        verify(ftpService).getInvoicesDirectory();
        verify(emailMessageService).sendInvoiceEmailWithBytes(
                eq(clientEmail),
                eq(COPY_EMAIL),
                eq("John"),
                eq(invoiceNumber),
                eq("January"),
                eq(invoiceNumber + ".pdf"),
                any(byte[].class)
        );

        verify(invoiceRepository).save(argThat(inv -> {
            return inv.getId().equals(invoiceId) &&
                    inv.getPdfPath().equals(ftpDirectory + "/" + invoiceNumber + ".pdf") &&
                    inv.getPdfGeneratedAt() != null &&
                    inv.getEmailSentAt() != null;
        }));
    }

    @Test
    void shouldThrowExceptionWhenInvoiceNotFoundDuringPdfGeneration() {
        // given
        Long invoiceId = 1L;
        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.empty());

        // when/then
        assertThatThrownBy(() -> invoiceDocumentService.savePdfAndSendInvoice(invoiceId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Invoice with id 1 not found");

        verify(invoiceRepository).findById(invoiceId);
        verifyNoInteractions(pdfGenerator, ftpService, emailMessageService);
    }

    @Test
    void shouldThrowExceptionWhenEmailSendingFails() throws MessagingException {
        // given
        Long invoiceId = 1L;
        String invoiceNumber = "001-01-2023";
        LocalDate issueDate = LocalDate.of(2023, Month.JANUARY, 15);
        String clientName = "John Doe";
        String clientEmail = "john@example.com";
        String ftpDirectory = "/invoices";

        Client client = new Client();
        client.setClientName(clientName);
        client.setEmail(clientEmail);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        invoice.setClient(client);

        byte[] pdfContent = "PDF content".getBytes();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.getInvoicesDirectory()).thenReturn(ftpDirectory);
        doNothing().when(pdfGenerator).generateInvoicePdf(eq(invoice), eq(seller), any(ByteArrayOutputStream.class));
        doNothing().when(ftpService).uploadPdfInvoice(eq(invoiceNumber + ".pdf"), any(byte[].class));
        doThrow(new MessagingException("Email sending failed"))
                .when(emailMessageService).sendInvoiceEmailWithBytes(
                        anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(byte[].class)
                );

        // when/then
        assertThatThrownBy(() -> invoiceDocumentService.savePdfAndSendInvoice(invoiceId))
                .isInstanceOf(EmailException.class)
                .hasMessageContaining("Failed to send invoice email for invoice 1");

        verify(invoiceRepository).findById(invoiceId);
        verify(pdfGenerator).generateInvoicePdf(eq(invoice), eq(seller), any(ByteArrayOutputStream.class));
        verify(ftpService).uploadPdfInvoice(eq(invoiceNumber + ".pdf"), any(byte[].class));
        verify(ftpService).getInvoicesDirectory();
        verify(emailMessageService).sendInvoiceEmailWithBytes(
                eq(clientEmail),
                eq(COPY_EMAIL),
                eq("John"),
                eq(invoiceNumber),
                eq("January"),
                eq(invoiceNumber + ".pdf"),
                any(byte[].class)
        );
        verify(invoiceRepository, never()).save(any(Invoice.class));
    }

    @Test
    void shouldHandleClientNameWithoutFirstName() throws MessagingException {
        // given
        Long invoiceId = 1L;
        String invoiceNumber = "001-01-2023";
        LocalDate issueDate = LocalDate.of(2023, Month.JANUARY, 15);
        String clientName = "CompanyName"; // No space, so no first name extraction
        String clientEmail = "info@company.com";
        String ftpDirectory = "/invoices";

        Client client = new Client();
        client.setClientName(clientName);
        client.setEmail(clientEmail);

        Invoice invoice = new Invoice();
        invoice.setId(invoiceId);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setIssueDate(issueDate);
        invoice.setClient(client);

        byte[] pdfContent = "PDF content".getBytes();

        when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(invoice));
        when(ftpService.getInvoicesDirectory()).thenReturn(ftpDirectory);
        doNothing().when(pdfGenerator).generateInvoicePdf(eq(invoice), eq(seller), any(ByteArrayOutputStream.class));
        doNothing().when(ftpService).uploadPdfInvoice(eq(invoiceNumber + ".pdf"), any(byte[].class));

        // when
        invoiceDocumentService.savePdfAndSendInvoice(invoiceId);

        // then
        verify(emailMessageService).sendInvoiceEmailWithBytes(
                eq(clientEmail),
                eq(COPY_EMAIL),
                eq("CompanyName"), // Should use full company name when no space exists
                eq(invoiceNumber),
                eq("January"),
                eq(invoiceNumber + ".pdf"),
                any(byte[].class)
        );

        verify(invoiceRepository).save(any(Invoice.class));
    }
}
