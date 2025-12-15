package dev.robgro.timesheet.tracking;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailTrackingControllerTest {

    @Mock
    private EmailTrackingService trackingService;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private EmailTrackingController controller;

    @Test
    void shouldReturnTransparentPngForValidToken() {
        // given
        String token = "valid-token-123";
        when(trackingService.recordEmailOpen(eq(token), any(HttpServletRequest.class))).thenReturn(true);

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EmailTrackingController.TRANSPARENT_PNG);

        verify(trackingService).recordEmailOpen(token, request);
    }

    @Test
    void shouldReturnTransparentPngForInvalidToken() {
        // given
        String token = "invalid-token";
        when(trackingService.recordEmailOpen(eq(token), any(HttpServletRequest.class))).thenReturn(false);

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        // IMPORTANT: Always return 200 OK even for invalid tokens (security)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EmailTrackingController.TRANSPARENT_PNG);

        verify(trackingService).recordEmailOpen(token, request);
    }

    @Test
    void shouldReturnPngEvenWhenServiceThrowsException() {
        // given
        String token = "error-token";
        when(trackingService.recordEmailOpen(eq(token), any(HttpServletRequest.class)))
                .thenThrow(new RuntimeException("Database error"));

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        // IMPORTANT: Never fail - always return pixel
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(EmailTrackingController.TRANSPARENT_PNG);
    }

    @Test
    void shouldSetCorrectContentType() {
        // given
        String token = "test-token";

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    void shouldSetNoCacheHeaders() {
        // given
        String token = "test-token";

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        HttpHeaders headers = response.getHeaders();

        String cacheControl = headers.getCacheControl();
        assertThat(cacheControl).isNotNull();
        assertThat(cacheControl)
                .containsAnyOf("no-cache", "no-store", "must-revalidate", "max-age=0");

        assertThat(headers.getExpires()).isEqualTo(0L);
        assertThat(headers.getPragma()).isEqualTo("no-cache");
    }

    @Test
    void shouldSetAntiCacheHeaders() {
        // given
        String token = "test-token";

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        HttpHeaders headers = response.getHeaders();

        assertThat(headers.get("X-Accel-Expires")).contains("0");
        assertThat(headers.get("Surrogate-Control")).contains("no-store");
    }

    @Test
    void shouldReturnValidPngBytes() {
        // given
        String token = "test-token";

        // when
        ResponseEntity<byte[]> response = controller.trackEmail(token, request);

        // then
        byte[] pngData = response.getBody();

        assertThat(pngData).isNotNull();
        assertThat(pngData).hasSize(67);

        // PNG signature: 89 50 4E 47 0D 0A 1A 0A
        assertThat(pngData[0]).isEqualTo((byte) 0x89);
        assertThat(pngData[1]).isEqualTo((byte) 0x50);
        assertThat(pngData[2]).isEqualTo((byte) 0x4E);
        assertThat(pngData[3]).isEqualTo((byte) 0x47);
    }

    @Test
    void shouldReturnOkForHealthCheck() {
        // when
        ResponseEntity<EmailTrackingController.TrackingHealthResponse> response = controller.health();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("OK");
        assertThat(response.getBody().message()).contains("operational");
        assertThat(response.getBody().timestamp()).isGreaterThan(0);
    }

    @Test
    void shouldInvokeServiceExactlyOnce() {
        // given
        String token = "test-token";

        // when
        controller.trackEmail(token, request);

        // then
        verify(trackingService, times(1)).recordEmailOpen(token, request);
    }

}
