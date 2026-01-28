package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.client.Client;
import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.invoice.InvoiceRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTrackingServiceTest {

    @Mock
    private EmailTrackingRepository trackingRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private EmailTrackingProperties trackingProperties;

    @Mock
    private EmailTrackingNotificationService notificationService;

    @Mock
    private EmailTrackingStatsService statsService;

    @InjectMocks
    private EmailTrackingServiceImpl emailTrackingService;

    @Mock
    private HttpServletRequest request;

    // ----- Token Creation Tests -----

    @Test
    void shouldCreateTrackingTokenSuccessfully() {
        // given
        Invoice invoice = new Invoice();
        invoice.setId(1L);

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.getTokenExpiryDays()).thenReturn(90);
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        String token = emailTrackingService.createTrackingToken(invoice);

        // then
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token).hasSize(36); // UUID length

        ArgumentCaptor<EmailTracking> trackingCaptor = ArgumentCaptor.forClass(EmailTracking.class);
        verify(trackingRepository).save(trackingCaptor.capture());

        EmailTracking savedTracking = trackingCaptor.getValue();
        assertThat(savedTracking.getInvoice()).isEqualTo(invoice);
        assertThat(savedTracking.getTrackingToken()).isEqualTo(token);
        assertThat(savedTracking.getExpiresAt()).isAfter(LocalDateTime.now());
        assertThat(savedTracking.getOpenCount()).isEqualTo(0);

        verify(invoiceRepository).save(invoice);
        assertThat(invoice.getEmailTrackingToken()).isEqualTo(token);
    }

    @Test
    void shouldReturnNullWhenTrackingDisabled() {
        // given
        Invoice invoice = new Invoice();
        when(trackingProperties.isEnabled()).thenReturn(false);

        // when
        String token = emailTrackingService.createTrackingToken(invoice);

        // then
        assertThat(token).isNull();
        verify(trackingRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldSetTokenExpiryTo90Days() {
        // given
        Invoice invoice = new Invoice();
        invoice.setId(1L);

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.getTokenExpiryDays()).thenReturn(90);
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        LocalDateTime beforeCreate = LocalDateTime.now();

        // when
        emailTrackingService.createTrackingToken(invoice);

        // then
        ArgumentCaptor<EmailTracking> trackingCaptor = ArgumentCaptor.forClass(EmailTracking.class);
        verify(trackingRepository).save(trackingCaptor.capture());

        EmailTracking savedTracking = trackingCaptor.getValue();
        LocalDateTime expectedExpiry = beforeCreate.plusDays(90);

        assertThat(savedTracking.getExpiresAt())
                .isAfter(expectedExpiry.minusSeconds(5))
                .isBefore(expectedExpiry.plusSeconds(5));
    }

    // ----- Email Open Recording Tests -----

    @Test
    void shouldRecordFirstEmailOpenSuccessfully() {
        // given
        String token = "test-token-123";

        Client client = new Client();
        client.setClientName("Test Client");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClient(client);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.isSendInstantReport()).thenReturn(false); // Disable notification
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/96.0");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        boolean isFirstOpen = emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(isFirstOpen).isTrue();

        verify(trackingRepository).save(tracking);
        verify(invoiceRepository).save(invoice);

        assertThat(tracking.getOpenedAt()).isNotNull();
        assertThat(tracking.getLastOpenedAt()).isNotNull();
        assertThat(tracking.getOpenCount()).isEqualTo(1);
        assertThat(tracking.getIpAddress()).isEqualTo("192.168.1.1");
        assertThat(tracking.getUserAgent()).contains("Chrome");
        assertThat(tracking.getDeviceType()).isEqualTo("Desktop");
    }

    @Test
    void shouldRecordReOpenSuccessfully() {
        // given
        String token = "test-token-123";

        Client client = new Client();
        client.setClientName("Test Client");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClient(client);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openedAt(LocalDateTime.now().minusHours(1)) // Already opened
                .openCount(1)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.isSendInstantReport()).thenReturn(false); // Disable notification
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)");
        when(request.getRemoteAddr()).thenReturn("192.168.1.2");
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        boolean isFirstOpen = emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(isFirstOpen).isFalse();

        verify(trackingRepository).save(tracking);
        verify(invoiceRepository).save(invoice);

        assertThat(tracking.getOpenCount()).isEqualTo(2);
        assertThat(tracking.getIpAddress()).isEqualTo("192.168.1.2");
        assertThat(tracking.getDeviceType()).isEqualTo("Mobile");
    }

    @Test
    void shouldNotRecordOpenWhenTokenExpired() {
        // given
        String token = "expired-token";
        Invoice invoice = new Invoice();
        invoice.setId(1L);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired!
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));

        // when
        boolean isFirstOpen = emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(isFirstOpen).isFalse();

        verify(trackingRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldNotRecordOpenWhenTokenNotFound() {
        // given
        String token = "invalid-token";

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.empty());

        // when
        boolean isFirstOpen = emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(isFirstOpen).isFalse();

        verify(trackingRepository, never()).save(any());
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void shouldNotRecordOpenWhenTrackingDisabled() {
        // given
        String token = "test-token";

        when(trackingProperties.isEnabled()).thenReturn(false);

        // when
        boolean isFirstOpen = emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(isFirstOpen).isFalse();

        verify(trackingRepository, never()).findByTrackingToken(any());
        verify(trackingRepository, never()).save(any());
    }

    // ----- IP Address Detection Tests -----

    @Test
    void shouldExtractIpFromXForwardedForHeader() {
        // given
        String token = "test-token";

        Client client = new Client();
        client.setClientName("Test Client");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClient(client);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.isSendInstantReport()).thenReturn(false);
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195, 70.41.3.18, 150.172.238.178");
        when(request.getHeader("User-Agent")).thenReturn("Test");
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(tracking.getIpAddress()).isEqualTo("203.0.113.195"); // First IP in X-Forwarded-For
    }

    @Test
    void shouldExtractIpFromXRealIpHeader() {
        // given
        String token = "test-token";

        Client client = new Client();
        client.setClientName("Test Client");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClient(client);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.isSendInstantReport()).thenReturn(false);
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.100");
        when(request.getHeader("User-Agent")).thenReturn("Test");
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(tracking.getIpAddress()).isEqualTo("203.0.113.100");
    }

    @Test
    void shouldFallbackToRemoteAddrWhenNoProxyHeaders() {
        // given
        String token = "test-token";

        Client client = new Client();
        client.setClientName("Test Client");

        Invoice invoice = new Invoice();
        invoice.setId(1L);
        invoice.setClient(client);

        EmailTracking tracking = EmailTracking.builder()
                .id(1L)
                .invoice(invoice)
                .trackingToken(token)
                .openCount(0)
                .expiresAt(LocalDateTime.now().plusDays(90))
                .build();

        when(trackingProperties.isEnabled()).thenReturn(true);
        when(trackingProperties.isSendInstantReport()).thenReturn(false);
        when(trackingRepository.findByTrackingToken(token)).thenReturn(Optional.of(tracking));
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Test");
        when(request.getRemoteAddr()).thenReturn("192.168.1.50");
        when(trackingRepository.save(any(EmailTracking.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

        // when
        emailTrackingService.recordEmailOpen(token, request);

        // then
        assertThat(tracking.getIpAddress()).isEqualTo("192.168.1.50");
    }
}
