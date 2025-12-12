package dev.robgro.timesheet.service;

import dev.robgro.timesheet.invoice.EmailMessageService;
import dev.robgro.timesheet.invoice.InvoiceEmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailMessageServiceTest {

    @Mock
    private JavaMailSender emailSender;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailMessageService emailMessageService;

    // Test data
    private InvoiceEmailRequest testRequest;

    @Captor
    private ArgumentCaptor<ByteArrayResource> resourceCaptor;

    @BeforeEach
    void setUp() {
        // given
        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);

        testRequest = InvoiceEmailRequest.builder()
                .recipientEmail("test@example.com")
                .ccEmail("cc@example.com")
                .firstName("John")
                .invoiceNumber("001-01-2025")
                .month("January")
                .fileName("invoice.pdf")
                .attachment("test content".getBytes())
                .numberOfVisits(12)
                .totalAmount(new BigDecimal("450.00"))
                .build();
    }

    @Test
    void sendInvoiceEmail_ShouldSendEmailSuccessfully() throws MessagingException {
        // given
        // Basic setup done in setUp()

        // when
        emailMessageService.sendInvoiceEmail(testRequest);

        // then
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmail_ShouldSetCorrectMessageParameters() throws MessagingException {
        // given
        MockedConstruction<MimeMessageHelper> helperMockedConstruction = mockConstruction(
                MimeMessageHelper.class,
                (mock, context) -> {
                     }
        );

        try {
            // when
            emailMessageService.sendInvoiceEmail(testRequest);

            // then
            assertEquals(1, helperMockedConstruction.constructed().size());

            MimeMessageHelper constructedHelper = helperMockedConstruction.constructed().get(0);

            verify(constructedHelper).setTo(testRequest.recipientEmail());
            verify(constructedHelper).setCc(testRequest.ccEmail());
            verify(constructedHelper).setSubject("Invoice " + testRequest.invoiceNumber() + " from Aga");

            verify(constructedHelper).setText(argThat(content ->
                    content.contains("Dear " + testRequest.firstName()) &&
                            content.contains(testRequest.invoiceNumber()) &&
                            content.contains(testRequest.month()) &&
                            content.contains(EmailMessageService.CONTACT_EMAIL)
            ), eq(true));

            verify(constructedHelper).addAttachment(eq(testRequest.fileName()), any(ByteArrayResource.class));
            verify(emailSender).send(mimeMessage);
        } finally {
            helperMockedConstruction.close();
        }
    }

    @Test
    void sendInvoiceEmail_ShouldAddCorrectAttachment() throws MessagingException {
        // given
        MockedConstruction<MimeMessageHelper> helperMockedConstruction = mockConstruction(
                MimeMessageHelper.class,
                (mock, context) -> {
                    doNothing().when(mock).addAttachment(eq(testRequest.fileName()), resourceCaptor.capture());
                }
        );

        try {
            // when
            emailMessageService.sendInvoiceEmail(testRequest);

            // then
            ByteArrayResource capturedResource = resourceCaptor.getValue();
            assertArrayEquals(testRequest.attachment(), capturedResource.getByteArray());
        } finally {
            helperMockedConstruction.close();
        }
    }

    @Test
    void sendInvoiceEmail_ShouldThrowExceptionWhenJavaMailSenderFails() throws MessagingException {
        // given
        doAnswer(invocation -> {
            throw new MessagingException("Failed to send email");
        }).when(emailSender).send(any(MimeMessage.class));

        // when & then
        assertThrows(MessagingException.class, () ->
                emailMessageService.sendInvoiceEmail(testRequest)
        );

        verify(emailSender).createMimeMessage();
        verify(emailSender).send(any(MimeMessage.class));
    }
}
