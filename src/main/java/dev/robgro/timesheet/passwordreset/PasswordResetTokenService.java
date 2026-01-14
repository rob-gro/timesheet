package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.InvalidTokenException;
import dev.robgro.timesheet.exception.TokenAlreadyUsedException;
import dev.robgro.timesheet.exception.TokenExpiredException;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for password reset token management.
 * Uses SHA-256 for token hashing (NOT BCrypt) for fast O(1) database lookups.
 * Tokens are single-use, short-lived, and stored as hashes only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.password-reset.token-ttl-minutes:30}")
    private int tokenTtlMinutes;

    @Value("${app.password-reset.token-ttl-minutes-self:15}")
    private int tokenTtlMinutesSelf;

    /**
     * Generate cryptographically strong reset token using SecureRandom.
     * @return 64-character hex string (32 bytes)
     */
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        random.nextBytes(tokenBytes);
        return Hex.encodeHexString(tokenBytes);
    }

    /**
     * Hash token with SHA-256 for database storage.
     * CRITICAL: NEVER use BCrypt for tokens - too slow for lookup.
     * SHA-256 allows fast O(1) lookup via UNIQUE index on token_hash.
     *
     * @param plainToken Plaintext token (64 hex chars)
     * @return SHA-256 hash (64 hex chars)
     */
    private String hashToken(String plainToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainToken.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Create password reset token for user.
     * Invalidates all previous unused tokens for the user.
     *
     * @param userId Target user ID
     * @param requestedBy ADMIN or SELF
     * @param requestIp Client IP address
     * @param userAgent Client User-Agent header
     * @return Plaintext token (ONLY returned here, never stored)
     */
    @Transactional
    public String createResetToken(Long userId, PasswordResetToken.ResetRequestType requestedBy,
                                   String requestIp, String userAgent) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User", userId));

        // Invalidate old unused tokens for this user
        invalidateUserTokens(userId);

        // Generate plaintext token (32 bytes = 64 hex chars)
        String plainToken = generateSecureToken();

        // Hash token with SHA-256 (NEVER BCrypt for tokens!)
        String tokenHash = hashToken(plainToken);

        // Determine TTL based on request type
        int ttl = requestedBy == PasswordResetToken.ResetRequestType.ADMIN
            ? tokenTtlMinutes
            : tokenTtlMinutesSelf;

        // Create entity
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(ttl));
        token.setRequestedBy(requestedBy);
        token.setRequestIp(requestIp);
        token.setUserAgent(userAgent);
        token.setResetVersion(user.getTokenVersion());

        tokenRepository.save(token);

        log.info("Password reset token created for user {} (requested by {})",
                 user.getUsername(), requestedBy);

        return plainToken;  // Return plaintext ONLY here, NEVER stored
    }

    /**
     * Validate reset token (READ-ONLY - no state mutation).
     * Uses SHA-256 hash for fast database lookup via UNIQUE index.
     *
     * IMPORTANT: This method does NOT consume the token.
     * - No usedAt updates
     * - No database writes
     * - Safe to call multiple times (e.g., from MVC GET /reset-password)
     * - Token consumption happens ONLY in consumeToken()
     *
     * @param plainToken Plaintext token from URL/request
     * @return PasswordResetToken if valid
     * @throws InvalidTokenException if token not found
     * @throws TokenAlreadyUsedException if token already used
     * @throws TokenExpiredException if token expired
     */
    @Transactional(readOnly = true)
    public PasswordResetToken validateToken(String plainToken) {
        // Hash plaintext token with SHA-256
        String queryHash = hashToken(plainToken);

        // Fast database lookup using UNIQUE index on token_hash
        PasswordResetToken token = tokenRepository.findByTokenHash(queryHash)
            .orElseThrow(() -> new InvalidTokenException("Invalid or expired token"));

        // Check if already used
        if (token.isUsed()) {
            throw new TokenAlreadyUsedException("Token has already been used");
        }

        // Check if expired
        if (token.isExpired()) {
            throw new TokenExpiredException("Token has expired");
        }

        return token;
    }

    /**
     * Consume token and reset user password.
     * Marks token as used, updates password, increments tokenVersion.
     *
     * @param plainToken Plaintext token
     * @param newPassword New password (plaintext, will be BCrypt encoded)
     * @throws InvalidTokenException if token not found
     * @throws TokenAlreadyUsedException if token already used
     * @throws TokenExpiredException if token expired
     */
    @Transactional
    public void consumeToken(String plainToken, String newPassword) {
        PasswordResetToken token = validateToken(plainToken);

        User user = token.getUser();

        // Update password with BCrypt (passwords use BCrypt, tokens use SHA-256)
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastPasswordResetAt(LocalDateTime.now());

        // Increment token version to invalidate all JWTs
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);

        // Mark token as used
        token.setUsedAt(LocalDateTime.now());
        tokenRepository.save(token);

        log.info("Password reset successful for user {} (token requested by {})",
                 user.getUsername(), token.getRequestedBy());
    }

    /**
     * Invalidate all unused tokens for user.
     * Sets usedAt to current time (semantically "revoked by new token request").
     *
     * NOTE: Uses usedAt field for revocation instead of separate revokedAt field.
     *
     * @param userId User ID
     */
    @Transactional
    public void invalidateUserTokens(Long userId) {
        List<PasswordResetToken> tokens = tokenRepository.findByUserIdAndUsedAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        tokens.forEach(t -> t.setUsedAt(now));
        tokenRepository.saveAll(tokens);

        if (!tokens.isEmpty()) {
            log.debug("Invalidated {} unused tokens for user {}", tokens.size(), userId);
        }
    }

    /**
     * Scheduled cleanup of old tokens (compliance/retention policy).
     * Runs daily at 2 AM.
     * Deletes tokens older than 7 days regardless of used status.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);
        int deleted = tokenRepository.deleteOldTokens(cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old password reset tokens", deleted);
        }
    }
}
