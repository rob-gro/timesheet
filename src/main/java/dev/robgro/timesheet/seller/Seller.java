package dev.robgro.timesheet.seller;

import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "sellers")
public class  Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "street", nullable = false)
    private String street;

    @Column(name = "postcode", nullable = false, length = 50)
    private String postcode;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "service_description", nullable = false)
    private String serviceDescription;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "sort_code", length = 10)
    private String sortCode;

    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "company_registration_number", length = 50)
    private String companyRegistrationNumber;

    @Column(name = "legal_form", length = 50)
    private String legalForm;

    @Column(name = "vat_number", length = 50)
    private String vatNumber;

    @Column(name = "tax_id", length = 50)
    private String taxId;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "is_system_default", nullable = false)
    private boolean isSystemDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @OneToMany(mappedBy = "seller")
    private List<Invoice> invoices;

    @OneToMany(mappedBy = "defaultSeller")
    private List<User> users;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return "Seller{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", street='" + street + '\'' +
                ", postcode='" + postcode + '\'' +
                ", city='" + city + '\'' +
                ", serviceDescription='" + serviceDescription + '\'' +
                ", bankName='" + bankName + '\'' +
                ", active=" + active +
                ", isSystemDefault=" + isSystemDefault +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Seller seller = (Seller) o;
        return active == seller.active &&
                isSystemDefault == seller.isSystemDefault &&
                Objects.equals(id, seller.id) &&
                Objects.equals(name, seller.name) &&
                Objects.equals(street, seller.street) &&
                Objects.equals(postcode, seller.postcode) &&
                Objects.equals(city, seller.city) &&
                Objects.equals(serviceDescription, seller.serviceDescription) &&
                Objects.equals(bankName, seller.bankName) &&
                Objects.equals(accountNumber, seller.accountNumber) &&
                Objects.equals(sortCode, seller.sortCode);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(id);
        result = 31 * result + Objects.hashCode(name);
        result = 31 * result + Objects.hashCode(street);
        result = 31 * result + Objects.hashCode(postcode);
        result = 31 * result + Objects.hashCode(city);
        result = 31 * result + Objects.hashCode(serviceDescription);
        result = 31 * result + Objects.hashCode(bankName);
        result = 31 * result + Objects.hashCode(accountNumber);
        result = 31 * result + Objects.hashCode(sortCode);
        result = 31 * result + Boolean.hashCode(active);
        result = 31 * result + Boolean.hashCode(isSystemDefault);
        return result;
    }
}
