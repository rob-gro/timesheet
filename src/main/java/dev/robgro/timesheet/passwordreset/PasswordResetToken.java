package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a password reset token with audit trail.
 * Tokens are stored as SHA-256 hashes (NEVER plaintext) for security.
 * Single-use tokens with short TTL (15-30 minutes).
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * SHA-256 hash of the reset token (64 hex characters).
     * NEVER store plaintext tokens in the database.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    /**
     * User who will reset their password.
     * Cascade delete: if user is deleted, all their reset tokens are deleted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * When the token was created.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * When the token expires (15-30 minutes after creation).
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * When the token was used (consumed) to reset password.
     * NULL = not used yet.
     * Also serves as "revoked" marker for invalidated tokens.
     */
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * Who requested the password reset: ADMIN or SELF.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_by", nullable = false, length = 10)
    private ResetRequestType requestedBy;

    /**
     * IP address of the requester (for audit trail).
     * Supports IPv6 (up to 45 characters).
     */
    @Column(name = "request_ip", length = 45)
    private String requestIp;

    /**
     * User-Agent header of the requester (for audit trail).
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Snapshot of user.tokenVersion at token creation time.
     * Used for JWT invalidation tracking.
     */
    @Column(name = "reset_version")
    private Integer resetVersion;

    /**
     * Enum for tracking who initiated the password reset.
     */
    public enum ResetRequestType {
        /**
         * Admin triggered the reset for the user.
         */
        ADMIN,

        /**
         * User requested the reset themselves (forgot password flow).
         */
        SELF
    }

    /**
     * Check if the token has expired.
     * @return true if current time is after expiresAt
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if the token has been used.
     * @return true if usedAt is not null
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Check if the token is valid (not expired and not used).
     * @return true if token can be used for password reset
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }
}
