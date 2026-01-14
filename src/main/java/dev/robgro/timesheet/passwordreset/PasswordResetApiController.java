package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.InvalidTokenException;
import dev.robgro.timesheet.exception.TokenAlreadyUsedException;
import dev.robgro.timesheet.exception.TokenExpiredException;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * REST API controller for password reset operations.
 * Handles both admin-initiated and self-service password resets.
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PasswordResetApiController {

    private final PasswordResetTokenService tokenService;
    private final PasswordResetEmailService emailService;
    private final UserRepository userRepository;
    private final RateLimitService rateLimitService;

    /**
     * Admin-initiated password reset.
     * No rate limiting by design (ADMIN role required, fully audited).
     *
     * WARNING: If this endpoint is ever made public or accessible to
     * non-admin roles, add rate limiting immediately!
     *
     * @param userId User ID to reset password for
     * @param request HTTP request for IP and User-Agent extraction
     * @return Success message
     */
    @PostMapping("/admin/users/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminResetPassword(
            @PathVariable Long userId,
            HttpServletRequest request) {

        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));

            String ip = getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            // Create token (no rate limit - ADMIN only, audited)
            String plainToken = tokenService.createResetToken(
                userId, PasswordResetToken.ResetRequestType.ADMIN, ip, userAgent);

            // Send email
            emailService.sendResetLinkEmail(
                user.getEmail(),
                user.getUsername(),
                plainToken,
                30  // Admin gets 30 min
            );

            return ResponseEntity.ok(Map.of(
                "message", "Password reset email sent"
            ));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));

        } catch (MessagingException e) {
            log.error("Failed to send password reset email", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to send email. Please try again."));
        }
    }

    /**
     * Self-service forgot password.
     * Rate limited per IP and email hash.
     *
     * @param dto Email address
     * @param request HTTP request for IP extraction
     * @return Generic success message (anti-enumeration)
     */
    @PostMapping("/auth/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordDto dto,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        String emailHash = hashEmail(dto.email());

        // Rate limiting
        if (rateLimitService.isRateLimited(ip, emailHash)) {
            log.warn("Rate limit exceeded for forgot password: ip={}, emailHash={}", ip, emailHash);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "Too many requests. Please try again later."));
        }

        rateLimitService.recordAttempt(ip, emailHash);

        // Generic response (anti-enumeration)
        String genericMessage = "If that email exists in our system, a reset link has been sent.";

        try {
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(dto.email());

            if (userOpt.isEmpty()) {
                // INTENTIONAL blocking delay (anti-enumeration timing attack)
                // Prevents attackers from timing response to determine if email exists
                Thread.sleep(ThreadLocalRandom.current().nextInt(100, 200));
                log.info("Forgot password request for non-existent email (hashed): {}", emailHash);
                return ResponseEntity.ok(Map.of("message", genericMessage));
            }

            User user = userOpt.get();
            String userAgent = request.getHeader("User-Agent");

            // Create token (shorter TTL for self-service)
            String plainToken = tokenService.createResetToken(
                user.getId(), PasswordResetToken.ResetRequestType.SELF, ip, userAgent);

            // Send email
            emailService.sendResetLinkEmail(
                user.getEmail(),
                user.getUsername(),
                plainToken,
                15  // Self-service gets 15 min
            );

            return ResponseEntity.ok(Map.of("message", genericMessage));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.ok(Map.of("message", genericMessage));

        } catch (Exception e) {
            log.error("Error during forgot password for email (hashed): {}", emailHash, e);
            // Still return generic message (don't leak errors)
            return ResponseEntity.ok(Map.of("message", genericMessage));
        }
    }

    /**
     * Reset password (consume token).
     *
     * @param dto Token and new password
     * @return Success message
     */
    @PostMapping("/auth/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordDto dto) {

        try {
            tokenService.consumeToken(dto.token(), dto.newPassword());

            return ResponseEntity.ok(Map.of(
                "message", "Password updated successfully. Please login with your new password."
            ));

        } catch (TokenAlreadyUsedException e) {
            // 409 CONFLICT - token was already consumed
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", e.getMessage()));

        } catch (InvalidTokenException | TokenExpiredException e) {
            // 404 NOT FOUND - token doesn't exist or has expired
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Error resetting password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "An error occurred. Please try again."));
        }
    }

    /**
     * Extract client IP address from request.
     * Checks X-Forwarded-For header for proxy/load balancer scenarios.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Hash email with SHA-256 for rate limiting.
     * Prevents leaking email existence in rate limit logs.
     */
    private String hashEmail(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(email.toLowerCase().getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
