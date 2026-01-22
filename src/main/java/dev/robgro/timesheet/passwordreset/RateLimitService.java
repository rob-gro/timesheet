package dev.robgro.timesheet.passwordreset;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting service for password reset requests.
 *
 * IMPORTANT: Single-instance implementation only.
 * Uses in-memory ConcurrentHashMap (not shared across app instances).
 * For multi-instance deployment, migrate to Redis-backed rate limiting.
 */
@Slf4j
@Service
public class RateLimitService {

    // Single-instance only - not shared across app nodes
    // For multi-instance, replace with Redis
    private final Map<String, List<Long>> ipAttempts = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> emailHashAttempts = new ConcurrentHashMap<>();

    private static final int MAX_IP_ATTEMPTS = 5;
    private static final int MAX_EMAIL_ATTEMPTS = 3;
    private static final long RATE_LIMIT_WINDOW_MS = 5 * 60 * 1000; // 5 minutes

    /**
     * Check if request is rate limited.
     * @param ip Client IP address
     * @param emailHash SHA-256 hash of email
     * @return true if rate limited
     */
    public boolean isRateLimited(String ip, String emailHash) {
        return isRateLimitedByKey(ipAttempts, ip, MAX_IP_ATTEMPTS) ||
               isRateLimitedByKey(emailHashAttempts, emailHash, MAX_EMAIL_ATTEMPTS);
    }

    /**
     * Record new attempt.
     * @param ip Client IP address
     * @param emailHash SHA-256 hash of email
     */
    public void recordAttempt(String ip, String emailHash) {
        recordAttemptForKey(ipAttempts, ip);
        recordAttemptForKey(emailHashAttempts, emailHash);
    }

    /**
     * Check if key is rate limited.
     * Thread-safe: uses Collections.synchronizedList.
     */
    private boolean isRateLimitedByKey(Map<String, List<Long>> cache, String key, int maxAttempts) {
        // Use computeIfAbsent to ensure thread-safe list creation and storage
        List<Long> attempts = cache.computeIfAbsent(
            key, k -> Collections.synchronizedList(new ArrayList<>())
        );

        long now = System.currentTimeMillis();

        // Remove expired attempts (synchronized list handles concurrent modifications)
        attempts.removeIf(timestamp -> now - timestamp > RATE_LIMIT_WINDOW_MS);

        return attempts.size() >= maxAttempts;
    }

    /**
     * Record attempt for key.
     * Thread-safe: uses Collections.synchronizedList.
     */
    private void recordAttemptForKey(Map<String, List<Long>> cache, String key) {
        // Thread-safe list creation - prevents ConcurrentModificationException
        cache.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>()))
             .add(System.currentTimeMillis());
    }

    /**
     * Cleanup expired attempts (runs every hour).
     * Prevents memory leaks in long-running applications.
     */
    @Scheduled(fixedRate = 3600000)  // Every hour
    public void cleanupExpiredAttempts() {
        long now = System.currentTimeMillis();

        ipAttempts.forEach((key, attempts) ->
            attempts.removeIf(timestamp -> now - timestamp > RATE_LIMIT_WINDOW_MS));

        emailHashAttempts.forEach((key, attempts) ->
            attempts.removeIf(timestamp -> now - timestamp > RATE_LIMIT_WINDOW_MS));

        log.debug("Rate limit cache cleaned up");
    }
}
