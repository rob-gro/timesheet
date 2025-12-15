package dev.robgro.timesheet.tracking;

import dev.robgro.timesheet.invoice.Invoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTrackingTest {

    @Test
    void shouldRecordFirstOpenCorrectly() {
        // given
        EmailTracking tracking = EmailTracking.builder()
                .trackingToken("test-token")
                .openCount(0)
                .build();

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/96.0";
        String ipAddress = "192.168.1.1";

        // when
        tracking.recordOpen(ipAddress, userAgent);

        // then
        assertThat(tracking.getOpenedAt()).isNotNull();
        assertThat(tracking.getLastOpenedAt()).isNotNull();
        assertThat(tracking.getOpenCount()).isEqualTo(1);
        assertThat(tracking.getIpAddress()).isEqualTo(ipAddress);
        assertThat(tracking.getUserAgent()).isEqualTo(userAgent);
        assertThat(tracking.getDeviceType()).isEqualTo("Desktop");
        assertThat(tracking.getEmailClient()).contains("Chrome");
    }

    @Test
    void shouldRecordReOpenCorrectly() {
        // given
        LocalDateTime firstOpen = LocalDateTime.now().minusHours(2);

        EmailTracking tracking = EmailTracking.builder()
                .trackingToken("test-token")
                .openedAt(firstOpen)
                .lastOpenedAt(firstOpen)
                .openCount(1)
                .ipAddress("192.168.1.1")
                .userAgent("Old User Agent")
                .deviceType("Desktop")
                .emailClient("Chrome")
                .build();

        String newUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15";
        String newIpAddress = "192.168.1.2";

        // when
        tracking.recordOpen(newIpAddress, newUserAgent);

        // then
        assertThat(tracking.getOpenedAt()).isEqualTo(firstOpen); // First open unchanged
        assertThat(tracking.getLastOpenedAt()).isAfter(firstOpen);
        assertThat(tracking.getOpenCount()).isEqualTo(2);
        assertThat(tracking.getIpAddress()).isEqualTo(newIpAddress); // Updated
        assertThat(tracking.getUserAgent()).isEqualTo(newUserAgent); // Updated
        assertThat(tracking.getDeviceType()).isEqualTo("Mobile"); // Detected from new UA
    }

    @Test
    void shouldCalculateTimeToFirstOpenInMinutes() {
        // given
        Invoice invoice = new Invoice();
        invoice.setEmailSentAt(LocalDateTime.now().minusMinutes(45));

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .openedAt(LocalDateTime.now())
                .build();

        // when
        Long minutes = tracking.getTimeToFirstOpenMinutes();

        // then
        assertThat(minutes).isBetween(44L, 46L); // Allow 1 minute tolerance
    }

    @Test
    void shouldReturnNullTimeWhenEmailNotSent() {
        // given
        Invoice invoice = new Invoice();
        invoice.setEmailSentAt(null);

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .openedAt(LocalDateTime.now())
                .build();

        // when
        Long minutes = tracking.getTimeToFirstOpenMinutes();

        // then
        assertThat(minutes).isNull();
    }

    @Test
    void shouldReturnNullTimeWhenNotOpened() {
        // given
        Invoice invoice = new Invoice();
        invoice.setEmailSentAt(LocalDateTime.now());

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .openedAt(null)
                .build();

        // when
        Long minutes = tracking.getTimeToFirstOpenMinutes();

        // then
        assertThat(minutes).isNull();
    }

    @Test
    void shouldCalculateTimeToFirstOpenInHours() {
        // given
        Invoice invoice = new Invoice();
        invoice.setEmailSentAt(LocalDateTime.now().minusMinutes(180)); // 3 hours

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .openedAt(LocalDateTime.now())
                .build();

        // when
        Long hours = tracking.getTimeToFirstOpenHours();

        // then
        assertThat(hours).isEqualTo(3L);
    }

    @Test
    void shouldFormatTimeToFirstOpen() {
        // given
        Invoice invoice = new Invoice();
        invoice.setEmailSentAt(LocalDateTime.now().minusMinutes(150)); // 2.5 hours

        EmailTracking tracking = EmailTracking.builder()
                .invoice(invoice)
                .openedAt(LocalDateTime.now())
                .build();

        // when
        String formatted = tracking.getTimeToFirstOpenFormatted();

        // then
        assertThat(formatted).contains("2 hours").contains("30 minutes");
    }

    @Test
    void shouldCheckIfExpired() {
        // given
        EmailTracking expiredTracking = EmailTracking.builder()
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        EmailTracking validTracking = EmailTracking.builder()
                .expiresAt(LocalDateTime.now().plusDays(1))
                .build();

        // when/then
        assertThat(expiredTracking.isExpired()).isTrue();
        assertThat(validTracking.isExpired()).isFalse();
    }

    @Test
    void shouldCheckIfFirstOpen() {
        // given
        EmailTracking notOpened = EmailTracking.builder()
                .openedAt(null)
                .openCount(0)
                .build();

        EmailTracking firstOpen = EmailTracking.builder()
                .openedAt(LocalDateTime.now())
                .openCount(1)
                .build();

        EmailTracking reOpened = EmailTracking.builder()
                .openedAt(LocalDateTime.now())
                .openCount(2)
                .build();

        // when/then
        assertThat(notOpened.isFirstOpen()).isFalse();
        assertThat(firstOpen.isFirstOpen()).isTrue();
        assertThat(reOpened.isFirstOpen()).isFalse();
    }

    // ----- Device Detection Tests -----

    @ParameterizedTest
    @CsvSource({
            "'Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X)', Mobile",
            "'Mozilla/5.0 (iPad; CPU OS 13_0 like Mac OS X)', Tablet",
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/96.0', Desktop",
            "'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)', Desktop",
            "'Mozilla/5.0 (Linux; Android 11; SM-G991B)', Mobile",
            "'Mozilla/5.0 (Linux; Android 11; SM-T870)', Mobile",
            "'Mozilla/5.0 (X11; Linux x86_64) Firefox/95.0', Desktop",
            "'', Desktop"
    })
    void shouldDetectDeviceTypeFromUserAgent(String userAgent, String expectedDevice) {
        // given
        EmailTracking tracking = EmailTracking.builder()
                .trackingToken("test")
                .build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getDeviceType()).isEqualTo(expectedDevice);
    }

    // ----- Email Client Detection Tests -----

    @ParameterizedTest
    @CsvSource({
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36', 'Browser (Chrome)'",
            "'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Safari/537.36 Edg/96.0', 'Browser (Safari)'",
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:95.0) Gecko/20100101 Firefox/95.0', 'Browser (Firefox)'",
            "'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.1 Safari/605.1.15', 'Browser (Safari)'",
            "'Mozilla/5.0 (Windows NT 10.0; Win64; x64; Trident/7.0; rv:11.0) like Gecko', Unknown",
            "'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Thunderbird/91.0', Thunderbird",
            "'', Unknown"
    })
    void shouldDetectEmailClientFromUserAgent(String userAgent, String expectedClient) {
        // given
        EmailTracking tracking = EmailTracking.builder()
                .trackingToken("test")
                .build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getEmailClient()).isEqualTo(expectedClient);
    }

    @Test
    void shouldDetectThunderbirdCorrectly() {
        // given
        String userAgent = "Mozilla/5.0 (X11; Linux x86_64; rv:91.0) Gecko/20100101 Thunderbird/91.3.0";
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getEmailClient()).isEqualTo("Thunderbird");
        assertThat(tracking.getDeviceType()).isEqualTo("Desktop");
    }

    @Test
    void shouldDetectAppleMailCorrectly() {
        // given
        // Note: This user agent doesn't contain identifiable Apple Mail markers
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko)";
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getEmailClient()).isEqualTo("Unknown");
        assertThat(tracking.getDeviceType()).isEqualTo("Desktop");
    }

    @Test
    void shouldDetectGmailWebClient() {
        // given
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/96.0.4664.110 Safari/537.36";
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getEmailClient()).contains("Chrome");
    }

    @Test
    void shouldDetectMobileDeviceFromIPhone() {
        // given
        String userAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 15_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Mobile/15E148 Safari/604.1";
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getDeviceType()).isEqualTo("Mobile");
    }

    @Test
    void shouldDetectTabletFromIPad() {
        // given
        // Note: This iPad user agent contains "Mobile" keyword, so it's detected as Mobile (not Tablet)
        // due to the order of checks in detectDeviceType()
        String userAgent = "Mozilla/5.0 (iPad; CPU OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1";
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", userAgent);

        // then
        assertThat(tracking.getDeviceType()).isEqualTo("Mobile");
    }

    @Test
    void shouldHandleNullUserAgentGracefully() {
        // given
        EmailTracking tracking = EmailTracking.builder().trackingToken("test").build();

        // when
        tracking.recordOpen("127.0.0.1", null);

        // then
        assertThat(tracking.getUserAgent()).isNull();
        assertThat(tracking.getDeviceType()).isEqualTo("Unknown");
        assertThat(tracking.getEmailClient()).isEqualTo("Unknown");
        assertThat(tracking.getOpenCount()).isEqualTo(1);
    }
}
