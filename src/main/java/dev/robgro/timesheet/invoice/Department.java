package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.seller.Seller;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Department within a seller organization (v1 - basic structure).
 * Used for multi-department invoice numbering (e.g., IT, HR, FIN, ... departments).
 *
 * Future enhancement - not fully implemented yet.
 * Database structure created now to avoid migration later.
 */
@Entity
@Getter
@NoArgsConstructor
@Table(
    name = "departments",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "unique_seller_code",
            columnNames = {"seller_id", "code"}
        )
    }
)
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    /**
     * Short department code used in invoice templates (e.g., "IT", "HR")
     * Must be unique within seller
     */
    @Column(nullable = false, length = 10)
    private String code;

    /**
     * Full department name (e.g., "Human Resources")
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Whether department is active and can be used for new invoices
     */
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Factory method to create a new department.
     *
     * @param seller Seller this department belongs to (required)
     * @param code Short code for templates (required, max 10 chars)
     * @param name Full department name (required, max 100 chars)
     * @return New active department
     * @throws ValidationException if validation fails
     */
    public static Department create(Seller seller, String code, String name) {
        if (seller == null) {
            throw new ValidationException("Seller is required");
        }
        if (code == null || code.isBlank()) {
            throw new ValidationException("Department code is required");
        }
        if (code.length() > 10) {
            throw new ValidationException("Department code too long (max 10 characters)");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Department name is required");
        }
        if (name.length() > 100) {
            throw new ValidationException("Department name too long (max 100 characters)");
        }

        Department department = new Department();
        department.seller = seller;
        department.code = code.toUpperCase(); // Normalize to uppercase
        department.name = name;
        department.active = true;
        return department;
    }

    /**
     * Deactivates this department.
     * Inactive departments cannot be used for new invoices.
     *
     * @throws BusinessRuleViolationException if already inactive
     */
    public void deactivate() {
        if (!this.active) {
            throw new BusinessRuleViolationException("Department is already inactive");
        }
        this.active = false;
    }

    /**
     * Activates this department.
     *
     * @throws BusinessRuleViolationException if already active
     */
    public void activate() {
        if (this.active) {
            throw new BusinessRuleViolationException("Department is already active");
        }
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
