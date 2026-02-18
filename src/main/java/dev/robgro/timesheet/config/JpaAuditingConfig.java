package dev.robgro.timesheet.config;

import dev.robgro.timesheet.security.SecurityUtils;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing configuration for automatic audit field management.
 *
 * <p>Enables automatic population of:
 * - {@code @CreatedBy} - user who created the entity
 * - {@code @CreatedDate} - timestamp when entity was created
 * - {@code @LastModifiedBy} - user who last modified the entity
 * - {@code @LastModifiedDate} - timestamp of last modification
 *
 * <p>Usage in entities:
 * <pre>
 * {@code
 * @Entity
 * @EntityListeners(AuditingEntityListener.class)
 * public class MyEntity {
 *     @CreatedBy
 *     @ManyToOne(fetch = FetchType.LAZY)
 *     private User createdBy;
 *
 *     @CreatedDate
 *     private LocalDateTime createdAt;
 *
 *     @LastModifiedDate
 *     private LocalDateTime updatedAt;
 * }
 * }
 * </pre>
 */
@Configuration
@EnableJpaAuditing
@RequiredArgsConstructor
public class JpaAuditingConfig {

    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /**
     * Provides current auditor (User) from Spring Security context.
     *
     * <p>Returns current authenticated user for @CreatedBy and @LastModifiedBy fields.
     * If no user is authenticated (e.g., system operations, migrations), returns empty.
     *
     * <p><b>CRITICAL FIX:</b> Uses FlushModeType.COMMIT to prevent infinite recursion.
     * Without this, loading user during preUpdate callback triggers autoFlush → infinite loop.
     *
     * @return Current user if authenticated, empty otherwise
     */
    @Bean
    public AuditorAware<User> auditorProvider() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return Optional.empty();
            }

            // Get username from authentication
            String username = authentication.getName();
            if (username == null || username.equals("anonymousUser")) {
                return Optional.empty();
            }

            // Load User entity from database with COMMIT flush mode
            // This prevents autoFlush during auditing callback → avoids StackOverflowError
            return entityManager.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                .setParameter("username", username)
                .setFlushMode(FlushModeType.COMMIT)  // ← CRITICAL: Disable autoFlush
                .getResultStream()
                .findFirst();
        };
    }
}
