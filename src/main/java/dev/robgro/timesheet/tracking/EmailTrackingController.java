package dev.robgro.timesheet.tracking;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * REST Controller for email tracking pixel
 *
 * This controller serves a 1x1 transparent PNG image that is embedded in invoice emails.
 * When a client opens the email, their email client requests this image, allowing us to
 * track email opens.
 *
 * IMPORTANT:
 * - Always returns 200 OK with a valid PNG (even for invalid tokens)
 * - Never returns 404 or error codes (to prevent token enumeration)
 * - Cache headers prevent image caching to ensure multiple opens are tracked
 * - Endpoint is public (no authentication required)
 */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
@Slf4j
public class EmailTrackingController {

    private final EmailTrackingService trackingService;

    /**
     * 1x1 transparent PNG image (89 bytes)
     * Generated from: data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==
     */
    public static final byte[] TRANSPARENT_PNG = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54,
            0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00, 0x05,
            0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00, 0x00,
            0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42,
            0x60, (byte) 0x82
    };

    /**
     * Tracking pixel endpoint
     * GET /api/track/{token}.png
     *
     * This endpoint is called when an email client loads the tracking pixel image.
     *
     * Security notes:
     * - Always returns 200 OK (never 404) to prevent token enumeration
     * - Logs invalid tokens for security monitoring
     * - No authentication required (must be publicly accessible)
     *
     * Performance notes:
     * - Tracking is recorded asynchronously to minimize response time
     * - Response time target: <50ms
     *
     * @param token The unique tracking token (UUID format)
     * @param request HTTP request containing IP and User-Agent
     * @return 1x1 transparent PNG with no-cache headers
     */
    @GetMapping("/{token}.png")
    public ResponseEntity<byte[]> trackEmail(
            @PathVariable String token,
            HttpServletRequest request) {

        log.debug("Tracking pixel requested: token={}, IP={}, User-Agent={}",
                token,
                request.getRemoteAddr(),
                request.getHeader("User-Agent"));

        // Record the tracking event (async - doesn't block response)
        try {
            trackingService.recordEmailOpen(token, request);
        } catch (Exception e) {
            // NEVER fail the request - always return the pixel
            // This ensures email clients don't retry and tracking still works
            log.error("Failed to record email open for token {}: {}",
                    token, e.getMessage(), e);
        }

        // Build response headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        // Prevent caching to ensure multiple opens are tracked
        // Without these headers, email clients (especially Gmail) will cache the image
        headers.setCacheControl(CacheControl.noCache()
                .noStore()
                .mustRevalidate()
                .cachePrivate()
                .maxAge(0, TimeUnit.SECONDS));

        headers.setExpires(0);
        headers.setPragma("no-cache");

        // Add cache buster prevention for Gmail proxy
        // Gmail's image proxy caches images - these headers help prevent that
        headers.add("X-Accel-Expires", "0");
        headers.add("Surrogate-Control", "no-store");

        // Always return 200 OK with 1x1 transparent PNG
        return new ResponseEntity<>(TRANSPARENT_PNG, headers, HttpStatus.OK);
    }

    /**
     * Health check endpoint for tracking service
     * GET /api/track/health
     *
     * Can be used to verify the tracking service is operational
     */
    @GetMapping("/health")
    public ResponseEntity<TrackingHealthResponse> health() {
        return ResponseEntity.ok(new TrackingHealthResponse(
                "OK",
                "Email tracking service is operational",
                System.currentTimeMillis()
        ));
    }

    /**
     * Response for health check endpoint
     */
    public record TrackingHealthResponse(
            String status,
            String message,
            long timestamp
    ) {
    }
}
