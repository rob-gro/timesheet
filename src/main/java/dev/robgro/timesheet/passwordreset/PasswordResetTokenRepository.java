package dev.robgro.timesheet.passwordreset;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for password reset token persistence.
 * Provides efficient lookups using SHA-256 hash with UNIQUE index.
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /**
     * Find token by SHA-256 hash.
     * Uses UNIQUE index on token_hash for O(1) constant-time lookup.
     * This is why we use SHA-256 instead of BCrypt for tokens.
     *
     * @param tokenHash SHA-256 hash of the plaintext token (64 hex characters)
     * @return Optional containing the token if found
     */
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /**
     * Find all unused tokens for a specific user.
     * Used for invalidating old tokens when creating a new one.
     *
     * @param userId User ID
     * @return List of unused tokens for the user
     */
    List<PasswordResetToken> findByUserIdAndUsedAtIsNull(Long userId);

    /**
     * Delete all expired tokens.
     * Used by scheduled cleanup job.
     *
     * @param cutoff Timestamp cutoff (tokens with expiresAt before this are deleted)
     * @return Number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.expiresAt < :cutoff")
    int deleteExpiredTokens(@Param("cutoff") LocalDateTime cutoff);

    /**
     * Delete all old tokens (regardless of used status).
     * Used for compliance/retention policy (e.g., delete tokens older than 7 days).
     *
     * @param cutoff Timestamp cutoff (tokens created before this are deleted)
     * @return Number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.createdAt < :cutoff")
    int deleteOldTokens(@Param("cutoff") LocalDateTime cutoff);
}
