package dev.robgro.timesheet.user;

import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.seller.Seller;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;

    private String email;
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_seller_id")
    private Seller defaultSeller;

    @Column(name = "last_password_changed_at")
    private LocalDateTime lastPasswordChangedAt;

    /**
     * Timestamp of last successful password reset via token link.
     * Used for audit trail.
     */
    @Column(name = "last_password_reset_at")
    private LocalDateTime lastPasswordResetAt;

    /**
     * Token version for JWT invalidation.
     * Incremented when password is reset to invalidate all existing JWTs.
     */
    @Column(name = "token_version", nullable = false)
    private Integer tokenVersion = 1;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * CRITICAL: Override Lombok @Data equals/hashCode to use business key (username).
     *
     * JPA entities should NOT use @Id in equals/hashCode because:
     * - id is null before persist
     * - hashCode changes after persist
     * - objects get "lost" in HashSet/HashMap
     *
     * Hibernate recommendation: Use business key (unique, never null).
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username != null && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        // Use business key (username) - consistent before and after persist
        return getClass().hashCode();
    }
}
