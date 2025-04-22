package dev.robgro.timesheet.service;

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
    private final String recipientEmail = "test@example.com";
    private final String ccEmail = "cc@example.com";
    private final String firstName = "John";
    private final String invoiceNumber = "001-01-2025";
    private final String month = "January";
    private final String fileName = "invoice.pdf";
    private final byte[] attachment = "test content".getBytes();

    @Captor
    private ArgumentCaptor<ByteArrayResource> resourceCaptor;

    @BeforeEach
    void setUp() {
        // given
        when(emailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    void sendInvoiceEmailWithBytes_ShouldSendEmailSuccessfully() throws MessagingException {
        // given
        // Basic setup done in setUp()

        // when
        emailMessageService.sendInvoiceEmailWithBytes(recipientEmail, ccEmail, firstName,
                invoiceNumber, month, fileName, attachment);

        // then
        verify(emailSender).createMimeMessage();
        verify(emailSender).send(mimeMessage);
    }

    @Test
    void sendInvoiceEmailWithBytes_ShouldSetCorrectMessageParameters() throws MessagingException {
        // given
        MockedConstruction<MimeMessageHelper> helperMockedConstruction = mockConstruction(
                MimeMessageHelper.class,
                (mock, context) -> {
                     }
        );

        try {
            // when
            emailMessageService.sendInvoiceEmailWithBytes(recipientEmail, ccEmail, firstName,
                    invoiceNumber, month, fileName, attachment);

            // then
            assertEquals(1, helperMockedConstruction.constructed().size());

            MimeMessageHelper constructedHelper = helperMockedConstruction.constructed().get(0);

            verify(constructedHelper).setTo(recipientEmail);
            verify(constructedHelper).setCc(ccEmail);
            verify(constructedHelper).setSubject("Invoice " + invoiceNumber + " from Aga");

            verify(constructedHelper).setText(argThat(content ->
                    content.contains("Hello " + firstName) &&
                            content.contains(invoiceNumber) &&
                            content.contains(month) &&
                            content.contains(EmailMessageService.CONTACT_EMAIL)
            ), eq(true));

            verify(constructedHelper).addAttachment(eq(fileName), any(ByteArrayResource.class));
            verify(emailSender).send(mimeMessage);
        } finally {
            helperMockedConstruction.close();
        }
    }

    @Test
    void sendInvoiceEmailWithBytes_ShouldAddCorrectAttachment() throws MessagingException {
        // given
        MockedConstruction<MimeMessageHelper> helperMockedConstruction = mockConstruction(
                MimeMessageHelper.class,
                (mock, context) -> {
                    doNothing().when(mock).addAttachment(eq(fileName), resourceCaptor.capture());
                }
        );

        try {
            // when
            emailMessageService.sendInvoiceEmailWithBytes(recipientEmail, ccEmail, firstName,
                    invoiceNumber, month, fileName, attachment);

            // then
            ByteArrayResource capturedResource = resourceCaptor.getValue();
            assertArrayEquals(attachment, capturedResource.getByteArray());
        } finally {
            helperMockedConstruction.close();
        }
    }

    @Test
    void sendInvoiceEmailWithBytes_ShouldThrowExceptionWhenJavaMailSenderFails() throws MessagingException {
        // given
        doAnswer(invocation -> {
            throw new MessagingException("Failed to send email");
        }).when(emailSender).send(any(MimeMessage.class));

        // when & then
        assertThrows(MessagingException.class, () ->
                emailMessageService.sendInvoiceEmailWithBytes(recipientEmail, ccEmail, firstName,
                        invoiceNumber, month, fileName, attachment)
        );

        verify(emailSender).createMimeMessage();
        verify(emailSender).send(any(MimeMessage.class));
    }
}
