# Seller Entity & Multi-Tenant SaaS Architecture Plan

**Date:** 2025-12-16
**Project:** Timesheet Invoice System
**Objective:** Replace hardcoded InvoiceSeller with dynamic Seller entity and prepare architecture for SaaS multi-tenancy

---

## Executive Summary

Current state: `InvoiceSeller` is hardcoded in configuration with static seller data. Bank details are hardcoded in PDF generator.

Goal: Transform into a flexible system where:
- **Phase 1**: Users can manage multiple sellers in a single database
- **Phase 2**: Each SaaS subscriber gets isolated database (true multi-tenancy)

**Recommended Approach:** Implement Phase 1 first (2-3 days), then Phase 2 later when ready to sell subscriptions (2-3 weeks additional work).

---

## PHASE 1: Seller Entity (Single Database Multi-Seller)

### Architecture Overview

**Pattern:** Follow existing Client entity pattern (active/inactive, CRUD operations)

**Database Strategy:** Single database with `seller_id` foreign keys in relevant tables

**User-Seller Relationship:** ManyToOne - one user can work with multiple sellers, selects seller when creating invoice

### 1.1 Database Schema Changes

#### New Table: `sellers`

```sql
CREATE TABLE sellers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    street VARCHAR(255) NOT NULL,
    postcode VARCHAR(50) NOT NULL,
    city VARCHAR(100) NOT NULL,

    -- Service description (what seller does)
    service_description VARCHAR(255) NOT NULL,  -- e.g., "Cleaning services", "Programming", "Trimming hedge", "Windows cleaning"

    -- Bank details (UK)
    bank_name VARCHAR(100),
    account_number VARCHAR(20),
    sort_code VARCHAR(10),

    -- Contact information
    email VARCHAR(255),
    phone VARCHAR(50),

    -- Company/Legal information
    company_registration_number VARCHAR(50),
    legal_form VARCHAR(50),  -- e.g., "Sole Trader", "Ltd", "PLC"

    -- Tax information (optional - different services may have different tax rates)
    vat_number VARCHAR(50) NULL,
    tax_id VARCHAR(50) NULL,

    -- Soft delete pattern
    active BOOLEAN NOT NULL DEFAULT TRUE,

    -- Audit fields
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,

    INDEX idx_active (active),
    INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### Modify Existing Tables

**invoices table:**
```sql
ALTER TABLE invoices
ADD COLUMN seller_id BIGINT,
ADD CONSTRAINT fk_invoice_seller
    FOREIGN KEY (seller_id) REFERENCES sellers(id);

CREATE INDEX idx_invoice_seller ON invoices(seller_id);
```

**timesheets table:** (REQUIRED - for automatic invoice generation via CRON)
```sql
ALTER TABLE timesheets
ADD COLUMN seller_id BIGINT NOT NULL,
ADD CONSTRAINT fk_timesheet_seller
    FOREIGN KEY (seller_id) REFERENCES sellers(id);

CREATE INDEX idx_timesheet_seller ON timesheets(seller_id);
```

**Rationale:** When CRON automatically generates monthly invoices, it needs to know which seller to assign. The default seller from User will be automatically set when creating timesheets.

**users table:** (for default seller preference)
```sql
ALTER TABLE users
ADD COLUMN default_seller_id BIGINT,
ADD CONSTRAINT fk_user_default_seller
    FOREIGN KEY (default_seller_id) REFERENCES sellers(id);
```

#### Migration: Default Seller (Agnieszka Markiewicz)

```sql
-- V4_create_seller_entity.sql
INSERT INTO sellers (
    name, street, postcode, city,
    service_description,
    bank_name, account_number, sort_code,
    email, phone,
    legal_form, active
) VALUES (
    'Agnieszka Markiewicz',
    '28 Ballater Place',
    'DD4 8SF',
    'Dundee',
    'Cleaning services',
    'TSB',
    '75040460',
    '87-68-20',
    NULL,
    '+44 747 8385 228',
    'Sole Trader',
    TRUE
);

-- Assign all existing invoices to default seller
UPDATE invoices
SET seller_id = (SELECT id FROM sellers WHERE name = 'Agnieszka Markiewicz' LIMIT 1)
WHERE seller_id IS NULL;

-- REQUIRED: Assign all existing timesheets to default seller
UPDATE timesheets
SET seller_id = (SELECT id FROM sellers WHERE name = 'Agnieszka Markiewicz' LIMIT 1)
WHERE seller_id IS NULL;

-- After migration, make seller_id NOT NULL
ALTER TABLE timesheets MODIFY seller_id BIGINT NOT NULL;
```

### 1.2 Backend Implementation

#### Entity: `Seller.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/Seller.java`

```java
package dev.robgro.timesheet.seller;

import dev.robgro.timesheet.invoice.Invoice;
import dev.robgro.timesheet.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@RequiredArgsConstructor
@Table(name = "sellers")
public class Seller {

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

    // Service description
    @Column(name = "service_description", nullable = false)
    private String serviceDescription;  // e.g., "Cleaning services", "Programming", "Trimming hedge"

    // Bank details
    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "sort_code", length = 10)
    private String sortCode;

    // Contact information
    @Column(name = "email")
    private String email;

    @Column(name = "phone", length = 50)
    private String phone;

    // Company/Legal information
    @Column(name = "company_registration_number", length = 50)
    private String companyRegistrationNumber;

    @Column(name = "legal_form", length = 50)
    private String legalForm;

    // Tax information (optional - different services may have different tax rates)
    @Column(name = "vat_number", length = 50)
    private String vatNumber;  // Nullable

    @Column(name = "tax_id", length = 50)
    private String taxId;  // Nullable

    // Soft delete
    @Column(name = "active", nullable = false)
    private boolean active = true;

    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    // Relationships
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
}
```

#### DTO: `SellerDto.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerDto.java`

```java
package dev.robgro.timesheet.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerDto(
    Long id,

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    String name,

    @NotBlank(message = "Street is required")
    String street,

    @NotBlank(message = "Postcode is required")
    String postcode,

    @NotBlank(message = "City is required")
    String city,

    @NotBlank(message = "Service description is required")
    String serviceDescription,

    String bankName,
    String accountNumber,
    String sortCode,

    @Email(message = "Email should be valid")
    String email,

    String phone,
    String companyRegistrationNumber,
    String legalForm,
    String vatNumber,
    String taxId,

    boolean active
) {}
```

#### Mapper: `SellerDtoMapper.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerDtoMapper.java`

```java
package dev.robgro.timesheet.seller;

import org.springframework.stereotype.Service;
import java.util.function.Function;

@Service
public class SellerDtoMapper implements Function<Seller, SellerDto> {

    @Override
    public SellerDto apply(Seller seller) {
        return new SellerDto(
            seller.getId(),
            seller.getName(),
            seller.getStreet(),
            seller.getPostcode(),
            seller.getCity(),
            seller.getBankName(),
            seller.getAccountNumber(),
            seller.getSortCode(),
            seller.getEmail(),
            seller.getPhone(),
            seller.getCompanyRegistrationNumber(),
            seller.getLegalForm(),
            seller.getVatNumber(),
            seller.getTaxId(),
            seller.isActive()
        );
    }
}
```

#### Repository: `SellerRepository.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerRepository.java`

```java
package dev.robgro.timesheet.seller;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SellerRepository extends JpaRepository<Seller, Long> {

    List<Seller> findByActiveTrue();

    @Query("SELECT s FROM Seller s WHERE " +
           "s.active = true AND " +
           "(:name IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%')))")
    List<Seller> findActiveSellersByName(@Param("name") String name);

    @Query("SELECT s FROM Seller s WHERE s.active = true ORDER BY s.name ASC")
    List<Seller> findAllActiveOrderByName();
}
```

#### Service Interface: `SellerService.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerService.java`

```java
package dev.robgro.timesheet.seller;

import java.util.List;

public interface SellerService {
    List<SellerDto> getAllSellers();
    SellerDto getSellerById(Long id);
    List<SellerDto> searchSellersByName(String name);
    SellerDto saveSeller(SellerDto sellerDto);
    SellerDto createSeller(SellerDto sellerDto);
    SellerDto updateSeller(Long id, SellerDto sellerDto);
    void deleteSeller(Long id);
    OperationResult deactivateSeller(Long id);
    SellerDto createEmptySellerDto();
}
```

#### Service Implementation: `SellerServiceImpl.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerServiceImpl.java`

```java
package dev.robgro.timesheet.seller;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerServiceImpl implements SellerService {

    private final SellerRepository sellerRepository;
    private final SellerDtoMapper sellerDtoMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SellerDto> getAllSellers() {
        return sellerRepository.findAllActiveOrderByName().stream()
                .map(sellerDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDto getSellerById(Long id) {
        return sellerDtoMapper.apply(getSellerOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SellerDto> searchSellersByName(String name) {
        return sellerRepository.findActiveSellersByName(name).stream()
                .map(sellerDtoMapper)
                .collect(Collectors.toList());
    }

    private Seller getSellerOrThrow(Long id) {
        return sellerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seller", id));
    }

    @Override
    public SellerDto saveSeller(SellerDto sellerDto) {
        if (sellerDto.id() == null) {
            return createSeller(sellerDto);
        } else {
            return updateSeller(sellerDto.id(), sellerDto);
        }
    }

    @Override
    public SellerDto createSeller(SellerDto sellerDto) {
        Seller seller = new Seller();
        seller.setActive(true);
        updateSellerFields(seller, sellerDto);
        return sellerDtoMapper.apply(sellerRepository.save(seller));
    }

    @Override
    public SellerDto updateSeller(Long id, SellerDto sellerDto) {
        Seller seller = getSellerOrThrow(id);
        updateSellerFields(seller, sellerDto);
        return sellerDtoMapper.apply(sellerRepository.save(seller));
    }

    private void updateSellerFields(Seller seller, SellerDto dto) {
        seller.setName(dto.name());
        seller.setStreet(dto.street());
        seller.setPostcode(dto.postcode());
        seller.setCity(dto.city());
        seller.setServiceDescription(dto.serviceDescription());
        seller.setBankName(dto.bankName());
        seller.setAccountNumber(dto.accountNumber());
        seller.setSortCode(dto.sortCode());
        seller.setEmail(dto.email());
        seller.setPhone(dto.phone());
        seller.setCompanyRegistrationNumber(dto.companyRegistrationNumber());
        seller.setLegalForm(dto.legalForm());
        seller.setVatNumber(dto.vatNumber());
        seller.setTaxId(dto.taxId());
    }

    @Transactional
    @Override
    public void deleteSeller(Long id) {
        Seller seller = sellerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Seller", id));
        seller.setActive(false);
        sellerRepository.save(seller);
        log.info("Seller with id {} has been deactivated", id);
    }

    @Transactional
    @Override
    public OperationResult deactivateSeller(Long id) {
        try {
            Seller seller = getSellerOrThrow(id);
            seller.setActive(false);
            sellerRepository.save(seller);
            log.info("Seller with id {} has been deactivated", id);
            return new OperationResult(true, "Seller has been successfully deactivated");
        } catch (Exception e) {
            log.error("Failed to deactivate seller with id: {}", id, e);
            return new OperationResult(false, "Unable to deactivate seller");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SellerDto createEmptySellerDto() {
        return new SellerDto(
                null, "", "", "", "",
                null, null, null, null, null,
                null, null, null, null, true
        );
    }
}
```

#### OperationResult record

**Location:** `src/main/java/dev/robgro/timesheet/seller/OperationResult.java`

```java
package dev.robgro.timesheet.seller;

public record OperationResult(boolean success, String message) {}
```

#### REST Controller: `SellerController.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerController.java`

```java
package dev.robgro.timesheet.seller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
public class SellerController {

    private final SellerService sellerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SellerDto>> getAllSellers() {
        return ResponseEntity.ok(sellerService.getAllSellers());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SellerDto> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.getSellerById(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SellerDto>> searchSellers(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(sellerService.searchSellersByName(name));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDto> createSeller(@Valid @RequestBody SellerDto sellerDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.createSeller(sellerDto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDto> updateSeller(@PathVariable Long id,
                                                    @Valid @RequestBody SellerDto sellerDto) {
        return ResponseEntity.ok(sellerService.updateSeller(id, sellerDto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OperationResult> deactivateSeller(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.deactivateSeller(id));
    }
}
```

#### MVC Controller: `SellerViewController.java`

**Location:** `src/main/java/dev/robgro/timesheet/seller/SellerViewController.java`

```java
package dev.robgro.timesheet.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/sellers")
@RequiredArgsConstructor
public class SellerViewController {

    private final SellerService sellerService;

    @GetMapping
    public String listSellers(Model model) {
        model.addAttribute("sellers", sellerService.getAllSellers());
        return "sellers/list";
    }

    @GetMapping("/new")
    public String newSellerForm(Model model) {
        model.addAttribute("seller", sellerService.createEmptySellerDto());
        return "sellers/form";
    }

    @GetMapping("/edit/{id}")
    public String editSellerForm(@PathVariable Long id, Model model) {
        model.addAttribute("seller", sellerService.getSellerById(id));
        return "sellers/form";
    }

    @PostMapping
    public String saveSeller(@ModelAttribute SellerDto sellerDto) {
        sellerService.saveSeller(sellerDto);
        return "redirect:/sellers";
    }

    @DeleteMapping("/{id}")
    public String deleteSeller(@PathVariable Long id) {
        sellerService.deactivateSeller(id);
        return "redirect:/sellers";
    }
}
```

### 1.3 Modify Existing Classes

#### Invoice.java - Add Seller relationship

**File:** `src/main/java/dev/robgro/timesheet/invoice/Invoice.java`

**Add field:**
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "seller_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
private Seller seller;
```

#### InvoiceDto.java - Add sellerId

**File:** `src/main/java/dev/robgro/timesheet/invoice/InvoiceDto.java`

**Add field to record:**
```java
Long sellerId,
String sellerName,  // for display purposes
```

#### InvoiceDtoMapper.java - Map seller

**File:** `src/main/java/dev/robgro/timesheet/invoice/InvoiceDtoMapper.java`

**Update mapping:**
```java
invoice.getSeller() != null ? invoice.getSeller().getId() : null,
invoice.getSeller() != null ? invoice.getSeller().getName() : null,
```

#### InvoiceCreationServiceImpl.java - Set seller

**File:** `src/main/java/dev/robgro/timesheet/invoice/InvoiceCreationServiceImpl.java`

**Modify `createInvoice()` method signature:**
```java
public InvoiceDto createInvoice(Long clientId, Long sellerId, LocalDate issueDate, List<Long> timesheetIds)
```

**Add:**
```java
// Validate seller exists
Seller seller = sellerRepository.findById(sellerId)
    .orElseThrow(() -> new EntityNotFoundException("Seller", sellerId));

invoice.setSeller(seller);
```

#### PdfGenerator.java - Use dynamic seller

**File:** `src/main/java/dev/robgro/timesheet/invoice/PdfGenerator.java`

**Current signature (line 24):**
```java
public void generateInvoicePdf(Invoice invoice, InvoiceSeller seller, OutputStream outputStream)
```

**Change to:**
```java
public void generateInvoicePdf(Invoice invoice, OutputStream outputStream)
```

**Replace hardcoded seller data (lines 88-90) with:**
```java
Seller seller = invoice.getSeller();
fromCell.addElement(new Paragraph(seller.getName(), boldFont));
fromCell.addElement(new Paragraph(seller.getStreet(), boldFont));
fromCell.addElement(new Paragraph(seller.getPostcode() + ", " + seller.getCity(), boldFont));
```

**Replace hardcoded bank details (lines 229-254) with:**
```java
Phrase holderValue = new Phrase(seller.getName(), boldFont);
// ...
Phrase bankValue = new Phrase(seller.getBankName(), boldFont);
// ...
Phrase sortCodeValue = new Phrase(seller.getSortCode(), boldFont);
// ...
Phrase accountValue = new Phrase(seller.getAccountNumber(), boldFont);
```

#### InvoiceDocumentServiceImpl.java - Remove InvoiceSeller injection

**File:** `src/main/java/dev/robgro/timesheet/invoice/InvoiceDocumentServiceImpl.java`

**Remove:**
```java
private final InvoiceSeller invoiceSeller;
```

**Update PDF generation call:**
```java
pdfGenerator.generateInvoicePdf(invoice, outputStream);  // Remove seller parameter
```

#### Delete: InvoiceSeller.java

**File:** `src/main/java/dev/robgro/timesheet/config/InvoiceSeller.java`

**Action:** Delete this file (no longer needed)

**Remove from application.properties:**
```properties
# DELETE these lines:
invoice.seller.name=...
invoice.seller.street=...
invoice.seller.postcode=...
invoice.seller.city=...
```

### 1.4 Frontend Implementation

#### Seller List View: `sellers/list.html`

**Location:** `src/main/resources/templates/sellers/list.html`

**Content:** Similar to `clients/list.html` - table with sellers, edit/delete buttons

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{head :: head}"></head>
<body>
<div class="container mt-4">
    <h2>Sellers</h2>
    <a th:href="@{/sellers/new}" class="btn btn-primary mb-3">Add New Seller</a>

    <table class="table table-striped">
        <thead>
            <tr>
                <th>Name</th>
                <th>Address</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Bank</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr th:each="seller : ${sellers}">
                <td th:text="${seller.name}"></td>
                <td th:text="${seller.street + ', ' + seller.postcode + ' ' + seller.city}"></td>
                <td th:text="${seller.email}"></td>
                <td th:text="${seller.phone}"></td>
                <td th:text="${seller.bankName}"></td>
                <td>
                    <a th:href="@{/sellers/edit/{id}(id=${seller.id})}" class="btn btn-sm btn-warning">Edit</a>
                    <form th:action="@{/sellers/{id}(id=${seller.id})}" method="post" style="display:inline;">
                        <input type="hidden" name="_method" value="delete"/>
                        <button type="submit" class="btn btn-sm btn-danger">Deactivate</button>
                    </form>
                </td>
            </tr>
        </tbody>
    </table>
</div>
</body>
</html>
```

#### Seller Form: `sellers/form.html`

**Location:** `src/main/resources/templates/sellers/form.html`

**Content:** Form with all seller fields (name, address, bank details, contact, company info)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{head :: head}"></head>
<body>
<div class="container mt-4">
    <h2 th:text="${seller.id == null ? 'New Seller' : 'Edit Seller'}"></h2>

    <form th:action="@{/sellers}" th:object="${seller}" method="post">
        <input type="hidden" th:field="*{id}"/>

        <!-- Basic Information -->
        <div class="card mb-3">
            <div class="card-header">Basic Information</div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Name *</label>
                    <input type="text" class="form-control" th:field="*{name}" required/>
                </div>
                <div class="mb-3">
                    <label class="form-label">Street *</label>
                    <input type="text" class="form-control" th:field="*{street}" required/>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Postcode *</label>
                        <input type="text" class="form-control" th:field="*{postcode}" required/>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">City *</label>
                        <input type="text" class="form-control" th:field="*{city}" required/>
                    </div>
                </div>
            </div>
        </div>

        <!-- Bank Details -->
        <div class="card mb-3">
            <div class="card-header">Bank Details</div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Bank Name</label>
                    <input type="text" class="form-control" th:field="*{bankName}"/>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Account Number</label>
                        <input type="text" class="form-control" th:field="*{accountNumber}"/>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Sort Code</label>
                        <input type="text" class="form-control" th:field="*{sortCode}" placeholder="00-00-00"/>
                    </div>
                </div>
            </div>
        </div>

        <!-- Contact Information -->
        <div class="card mb-3">
            <div class="card-header">Contact Information</div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Email</label>
                    <input type="email" class="form-control" th:field="*{email}"/>
                </div>
                <div class="mb-3">
                    <label class="form-label">Phone</label>
                    <input type="text" class="form-control" th:field="*{phone}"/>
                </div>
            </div>
        </div>

        <!-- Company/Tax Information -->
        <div class="card mb-3">
            <div class="card-header">Company & Tax Information</div>
            <div class="card-body">
                <div class="mb-3">
                    <label class="form-label">Legal Form</label>
                    <select class="form-control" th:field="*{legalForm}">
                        <option value="">Select...</option>
                        <option value="Sole Trader">Sole Trader</option>
                        <option value="Ltd">Limited Company (Ltd)</option>
                        <option value="PLC">Public Limited Company (PLC)</option>
                        <option value="LLP">Limited Liability Partnership (LLP)</option>
                        <option value="Partnership">Partnership</option>
                    </select>
                </div>
                <div class="mb-3">
                    <label class="form-label">Company Registration Number</label>
                    <input type="text" class="form-control" th:field="*{companyRegistrationNumber}"/>
                </div>
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label">VAT Number</label>
                        <input type="text" class="form-control" th:field="*{vatNumber}"/>
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label">Tax ID</label>
                        <input type="text" class="form-control" th:field="*{taxId}"/>
                    </div>
                </div>
            </div>
        </div>

        <button type="submit" class="btn btn-primary">Save</button>
        <a th:href="@{/sellers}" class="btn btn-secondary">Cancel</a>
    </form>
</div>
</body>
</html>
```

#### Update Navigation: Add Settings menu with Sellers

**File:** `src/main/resources/templates/head.html` (or wherever navigation is)

**Replace direct "Sellers" link with "Settings" dropdown:**
```html
<li class="nav-item dropdown">
    <a class="nav-link dropdown-toggle" href="#" id="settingsDropdown"
       role="button" data-bs-toggle="dropdown" aria-expanded="false">
        Settings
    </a>
    <ul class="dropdown-menu" aria-labelledby="settingsDropdown">
        <li><a class="dropdown-item" th:href="@{/sellers}">Sellers</a></li>
        <!-- Future settings items will be added here -->
        <!-- <li><a class="dropdown-item" th:href="@{/settings/other}">Other Settings</a></li> -->
    </ul>
</li>
```

**Rationale:** Using "Settings" menu provides a scalable structure for future configuration options (e.g., email templates, notification preferences, API keys, etc.)

#### Modify Invoice Creation Form: Add Seller dropdown

**File:** `src/main/resources/templates/invoices/create.html`

**Add before client selection:**
```html
<div class="mb-3">
    <label class="form-label">Seller *</label>
    <select class="form-control" name="sellerId" required>
        <option value="">Select seller...</option>
        <option th:each="seller : ${sellers}"
                th:value="${seller.id}"
                th:text="${seller.name}"></option>
    </select>
</div>
```

**Update controller to pass sellers:**

**File:** `src/main/java/dev/robgro/timesheet/invoice/InvoiceCreateController.java`

```java
model.addAttribute("sellers", sellerService.getAllSellers());
```

### 1.5 Testing

#### Unit Test: `SellerServiceImplTest.java`

**Location:** `src/test/java/dev/robgro/timesheet/service/SellerServiceImplTest.java`

**Pattern:** Follow `ClientServiceImplTest.java` structure

```java
@ExtendWith(MockitoExtension.class)
class SellerServiceImplTest {
    @Mock private SellerRepository sellerRepository;
    @Mock private SellerDtoMapper sellerDtoMapper;
    @InjectMocks private SellerServiceImpl sellerService;

    @Test
    void shouldGetAllSellers() {
        // Given: active sellers exist
        // When: getAllSellers()
        // Then: return only active sellers
    }

    @Test
    void shouldDeactivateSellerInsteadOfDelete() {
        // Given: seller with id=1 exists
        // When: deleteSeller(1)
        // Then: seller.active = false, not deleted
    }

    // ... more tests
}
```

#### Test Data Builder: `SellerTestDataBuilder.java`

**Location:** `src/test/java/dev/robgro/timesheet/fixtures/SellerTestDataBuilder.java`

```java
public class SellerTestDataBuilder {
    private Long id = 1L;
    private String name = "Test Seller Ltd";
    private String street = "123 Test Street";
    private String postcode = "TE1 1ST";
    private String city = "TestCity";
    private String serviceDescription = "Test Services";
    private String bankName = "Test Bank";
    private String accountNumber = "12345678";
    private String sortCode = "12-34-56";
    private boolean active = true;

    public static SellerTestDataBuilder aSeller() {
        return new SellerTestDataBuilder();
    }

    public SellerTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public SellerTestDataBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public SellerTestDataBuilder inactive() {
        this.active = false;
        return this;
    }

    public Seller build() {
        Seller seller = new Seller();
        seller.setId(id);
        seller.setName(name);
        seller.setStreet(street);
        seller.setPostcode(postcode);
        seller.setCity(city);
        seller.setServiceDescription(serviceDescription);
        seller.setBankName(bankName);
        seller.setAccountNumber(accountNumber);
        seller.setSortCode(sortCode);
        seller.setActive(active);
        return seller;
    }
}
```

### 1.6 Security Configuration Updates

**File:** `src/main/java/dev/robgro/timesheet/security/SecurityConfig.java`

**Add seller endpoints to authorization rules:**
```java
.requestMatchers("/api/v1/sellers/**").hasAnyRole("ADMIN", "USER")
.requestMatchers("/sellers/**").hasAnyRole("ADMIN", "USER")
```

### 1.7 Migration Execution Order

1. **V4_create_seller_entity.sql** - Create sellers table
2. **V5_add_seller_to_invoices.sql** - Add seller_id to invoices
3. **V6_add_seller_to_timesheets.sql** - Add seller_id to timesheets (optional)
4. **V7_add_default_seller_to_users.sql** - Add default_seller_id to users
5. **V8_migrate_existing_data.sql** - Insert default seller and update existing records

### 1.8 Phase 1 Summary

**What we achieve:**
- ✅ Dynamic seller management (CRUD operations)
- ✅ Multiple sellers per database
- ✅ Soft delete pattern (active/inactive)
- ✅ Full bank details, contact info, company/tax info
- ✅ Seller selection during invoice creation
- ✅ PDF generation uses dynamic seller data
- ✅ Migration preserves existing data

**Limitations:**
- ❌ No tenant isolation (all users see all sellers)
- ❌ No database-per-subscriber
- ❌ No automatic database provisioning

**Time estimate:** 2-3 days for experienced Spring Boot developer

---

## PHASE 2: Multi-Tenant SaaS (Database-Per-Tenant)

### Architecture Overview

**Pattern:** Database-per-tenant with dynamic datasource routing

**Tenant Resolution:** Subdomain-based (e.g., `company1.timesheet.robgro.dev`)

**Database Strategy:** Each subscriber gets isolated database (e.g., `robgro_tenant_abc123_invoices`)

### 2.1 High-Level Architecture

```
User Request → Subdomain → Tenant Resolver → Tenant Context → DataSource Router → Tenant DB
```

**Components:**
1. **Tenant Resolver** - Extracts tenant identifier from subdomain/JWT
2. **Tenant Context** - ThreadLocal storage for current tenant
3. **Tenant DataSource** - Manages connection pools per tenant
4. **DataSource Router** - Routes queries to correct database
5. **Tenant Registry** - Tracks active tenants and their DB configs
6. **Database Provisioner** - Creates new databases on subscription

### 2.2 Tenant Resolution Strategy

#### Option A: Subdomain-based (Recommended)

**URL Pattern:** `https://company1.timesheet.robgro.dev`

**Pros:**
- Clean separation
- Easy to cache (CDN)
- Professional appearance
- No token parsing on every request

**Cons:**
- Requires wildcard DNS
- Requires wildcard SSL certificate

#### Option B: JWT Claim-based

**Token contains:** `{ "username": "john", "tenantId": "abc123" }`

**Pros:**
- No DNS changes
- Works with mobile apps
- Single domain

**Cons:**
- Token parsing overhead
- Harder to cache
- Shared session issues

**Recommendation:** Use **Subdomain** for web app, **JWT claim** for mobile API

### 2.3 Database Naming Convention

**Pattern:** `robgro_tenant_{TENANT_ID}_invoices`

**Example:**
- Tenant ID: `abc123` → Database: `robgro_tenant_abc123_invoices`
- Tenant ID: `xyz789` → Database: `robgro_tenant_xyz789_invoices`

**Why not tenant name?**
- Tenant can change company name
- Avoid special characters/spaces in DB names
- Unique identifier (UUID or short hash)

### 2.4 Core Classes for Multi-Tenancy

#### Tenant Entity: `Tenant.java`

```java
@Entity
@Table(name = "tenants")  // In MASTER database
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String tenantId;  // e.g., "abc123"

    @Column(unique = true, nullable = false)
    private String subdomain;  // e.g., "company1"

    private String companyName;
    private String ownerEmail;

    // Database connection details
    private String databaseName;
    private String databaseHost;
    private Integer databasePort;
    private String databaseUsername;
    private String databasePassword;  // Encrypted

    // Subscription details
    private LocalDate subscriptionStartDate;
    private LocalDate subscriptionEndDate;
    private boolean active;
    private String subscriptionTier;  // "BASIC", "PRO", "ENTERPRISE"

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

#### Tenant Context: `TenantContext.java`

```java
public class TenantContext {
    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
```

#### Tenant Resolver: `TenantResolver.java`

```java
@Component
public class TenantResolver {

    public String resolveTenantId(HttpServletRequest request) {
        // Extract from subdomain
        String serverName = request.getServerName();

        if (serverName.contains(".timesheet.robgro.dev")) {
            String subdomain = serverName.split("\\.")[0];
            return tenantService.getTenantIdBySubdomain(subdomain);
        }

        // Fallback: extract from JWT token
        String token = extractTokenFromRequest(request);
        if (token != null) {
            return jwtTokenProvider.getTenantIdFromToken(token);
        }

        throw new TenantNotFoundException("Unable to resolve tenant");
    }
}
```

#### Tenant Interceptor: `TenantInterceptor.java`

```java
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Autowired
    private TenantResolver tenantResolver;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String tenantId = tenantResolver.resolveTenantId(request);
        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        TenantContext.clear();
    }
}
```

#### Dynamic DataSource Router: `MultiTenantDataSource.java`

```java
public class MultiTenantDataSource extends AbstractRoutingDataSource {

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getTenantId();
    }
}
```

#### DataSource Configuration: `MultiTenantConfig.java`

```java
@Configuration
public class MultiTenantConfig {

    @Bean
    public DataSource dataSource(TenantService tenantService) {
        MultiTenantDataSource dataSource = new MultiTenantDataSource();

        // Load all tenant datasources
        Map<Object, Object> targetDataSources = new HashMap<>();
        List<Tenant> tenants = tenantService.getAllActiveTenants();

        for (Tenant tenant : tenants) {
            DataSource tenantDataSource = createDataSource(tenant);
            targetDataSources.put(tenant.getTenantId(), tenantDataSource);
        }

        dataSource.setTargetDataSources(targetDataSources);
        dataSource.afterPropertiesSet();

        return dataSource;
    }

    private DataSource createDataSource(Tenant tenant) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:mariadb://%s:%d/%s",
            tenant.getDatabaseHost(),
            tenant.getDatabasePort(),
            tenant.getDatabaseName()));
        config.setUsername(tenant.getDatabaseUsername());
        config.setPassword(decryptPassword(tenant.getDatabasePassword()));
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
    }
}
```

### 2.5 Database Provisioning

#### Database Provisioner Service: `DatabaseProvisionerService.java`

```java
@Service
public class DatabaseProvisionerService {

    @Value("${master.datasource.url}")
    private String masterDbUrl;

    public void provisionTenantDatabase(String tenantId) {
        String databaseName = "robgro_tenant_" + tenantId + "_invoices";

        // 1. Create database
        try (Connection conn = DriverManager.getConnection(masterDbUrl)) {
            Statement stmt = conn.createStatement();
            stmt.execute("CREATE DATABASE " + databaseName +
                        " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");

            // 2. Create dedicated user
            String username = "tenant_" + tenantId;
            String password = generateSecurePassword();
            stmt.execute(String.format(
                "CREATE USER '%s'@'%%' IDENTIFIED BY '%s'",
                username, password));
            stmt.execute(String.format(
                "GRANT ALL PRIVILEGES ON %s.* TO '%s'@'%%'",
                databaseName, username));
            stmt.execute("FLUSH PRIVILEGES");

            // 3. Run Flyway migrations
            Flyway flyway = Flyway.configure()
                .dataSource(buildDataSourceForTenant(databaseName, username, password))
                .locations("classpath:db/migration")
                .load();
            flyway.migrate();

            // 4. Seed default data (default seller for this tenant)
            seedDefaultData(databaseName, username, password);

        } catch (SQLException e) {
            throw new DatabaseProvisioningException("Failed to provision database", e);
        }
    }

    private void seedDefaultData(String dbName, String user, String pass) {
        // Insert default seller using tenant owner's information
        // This runs against the NEW tenant database
    }
}
```

### 2.6 Registration & Onboarding Flow

**Sequence:**

1. User registers → `POST /api/auth/register`
2. Payment verification (Stripe/PayPal webhook)
3. Generate unique `tenantId` (UUID short form)
4. Assign subdomain (e.g., `company-abc123`)
5. `DatabaseProvisionerService.provisionTenantDatabase(tenantId)`
6. Save `Tenant` entity to MASTER database
7. Add tenant datasource to `MultiTenantDataSource`
8. Send welcome email with subdomain URL
9. User logs in at `https://company-abc123.timesheet.robgro.dev`

### 2.7 Master Database vs Tenant Databases

**Master Database:** `robgro_master_saas`
- `tenants` - Tenant registry
- `users` - All user accounts (with tenantId FK)
- `roles` - Shared roles
- `subscription_plans` - Available plans
- `billing_history` - Payment records

**Tenant Databases:** `robgro_tenant_{ID}_invoices`
- `sellers` - This tenant's sellers
- `clients` - This tenant's clients
- `timesheets` - This tenant's work entries
- `invoices` - This tenant's invoices
- `invoice_items` - Invoice line items

**Why this split?**
- Centralized user authentication (single JWT issuer)
- Centralized billing
- Tenant data fully isolated
- Easy to export/backup per tenant
- Easy to delete tenant (drop database)

### 2.8 Security Considerations

**Tenant Isolation:**
- Filter at DataSource level (impossible to access other tenant's data)
- JWT contains `tenantId` - validated on every request
- Admin cannot switch tenants (unless super-admin)

**SQL Injection Prevention:**
- Parameterized queries for tenant lookup
- Whitelist tenant IDs (alphanumeric only)
- No dynamic SQL for datasource creation

**Connection Pooling:**
- Limit max connections per tenant (e.g., 10)
- Monitor for connection leaks
- Implement circuit breaker for failing tenants

### 2.9 Challenges & Solutions

| Challenge | Solution |
|-----------|----------|
| **Cold start** - First request slow due to datasource creation | Warm up datasources on app startup for active tenants |
| **Connection pool exhaustion** | Implement max connections per tenant + circuit breaker |
| **Tenant onboarding latency** | Async database provisioning with status polling endpoint |
| **Backup complexity** | Automated per-tenant backups using `mysqldump` scheduled job |
| **Schema migrations** | Flyway runs against all tenant databases (iterate through registry) |
| **Tenant deactivation** | Mark tenant inactive, keep DB for 90 days, then drop |
| **Cross-tenant reporting** | Separate analytics database with data aggregated via ETL |

### 2.10 Migration from Phase 1 to Phase 2

**Step-by-step:**

1. **Create Master Database**
   - Create `robgro_master_saas` database
   - Run migrations for `tenants`, `users`, `roles` tables

2. **Migrate Existing Users**
   - Create first tenant (e.g., "legacy")
   - Move existing users to master DB with `tenant_id = legacy_tenant_id`

3. **Rename Existing Database**
   - Rename `robgro_aga_invoices` → `robgro_tenant_legacy_invoices`

4. **Update Application Configuration**
   - Switch from single datasource to `MultiTenantDataSource`
   - Enable tenant interceptor

5. **Deploy & Test**
   - Test legacy tenant at `https://aga.timesheet.robgro.dev`
   - Create new test tenant to verify provisioning

6. **Enable Public Registration**
   - Open registration endpoint
   - Integrate payment gateway
   - Enable automated provisioning

### 2.11 Cost Estimation

**Infrastructure:**
- Single MariaDB server can handle 50-100 tenant databases
- Each database ~50-100MB (small tenants)
- Consider database sharding at 500+ tenants

**Development Time:**
- Multi-tenant architecture: 2-3 weeks
- Database provisioning: 1 week
- Testing & security audit: 1 week
- Registration & payment integration: 1 week
- **Total: 5-6 weeks**

---

## Implementation Workflow

**IMPORTANT:** All work will be done on a separate branch `feature/seller-entity`. Each step requires explicit approval before proceeding.

### Git Branch Strategy

```bash
# Create feature branch from master
git checkout master
git pull origin master
git checkout -b feature/seller-entity
```

**Workflow:**
1. Present plan for specific step
2. Wait for user approval
3. Implement the step
4. Commit changes with clear message
5. Show result and ask for approval to continue
6. Repeat for next step
7. Final PR review before merge to master

### Phase 1 Implementation Steps (Incremental Approval Required)

**Each step below requires your approval before proceeding to the next one.**

#### Step 1: Database Migrations (Foundation)
- [ ] Create V4_create_seller_entity.sql
- [ ] Create V5_add_seller_to_invoices.sql
- [ ] Create V6_add_seller_to_timesheets.sql
- [ ] Create V7_add_default_seller_to_users.sql
- [ ] Create V8_migrate_existing_data.sql
- **Commit:** `feat: add database migrations for Seller entity`
- **Approval checkpoint:** Review migrations before executing

#### Step 2: Core Seller Entity & Repository
- [ ] Create Seller.java entity
- [ ] Create SellerDto.java record
- [ ] Create SellerDtoMapper.java
- [ ] Create SellerRepository.java interface
- [ ] Create OperationResult.java record
- **Commit:** `feat: add Seller entity, DTO, mapper, and repository`
- **Approval checkpoint:** Review domain model

#### Step 3: Seller Service Layer
- [ ] Create SellerService.java interface
- [ ] Create SellerServiceImpl.java with CRUD operations
- [ ] Follow Client pattern (active/inactive)
- **Commit:** `feat: implement SellerService with CRUD operations`
- **Approval checkpoint:** Review business logic

#### Step 4: Seller REST API
- [ ] Create SellerController.java (REST endpoints)
- [ ] Add security configuration for /api/v1/sellers
- **Commit:** `feat: add REST API for Seller management`
- **Approval checkpoint:** Test API endpoints with Postman/Swagger

#### Step 5: Modify Invoice to Use Seller
- [ ] Add seller field to Invoice.java entity
- [ ] Update InvoiceDto.java (add sellerId, sellerName)
- [ ] Update InvoiceDtoMapper.java
- [ ] Update InvoiceCreationServiceImpl.java (add sellerId parameter)
- **Commit:** `feat: add Seller relationship to Invoice entity`
- **Approval checkpoint:** Review entity relationships

#### Step 6: Update PDF Generation
- [ ] Modify PdfGenerator.java to use Invoice.getSeller()
- [ ] Remove InvoiceSeller parameter from method signature
- [ ] Update InvoiceDocumentServiceImpl.java
- [ ] Delete InvoiceSeller.java config class
- [ ] Remove invoice.seller.* from application.properties
- **Commit:** `refactor: migrate PdfGenerator to use dynamic Seller`
- **Approval checkpoint:** Test PDF generation

#### Step 7: Frontend - Seller Management UI
- [ ] Create SellerViewController.java (MVC controller)
- [ ] Create templates/sellers/list.html
- [ ] Create templates/sellers/form.html
- [ ] Update navigation (Settings dropdown in head.html)
- **Commit:** `feat: add Seller management UI`
- **Approval checkpoint:** Test UI in browser

#### Step 8: Update Invoice Creation UI
- [ ] Modify templates/invoices/create.html (add seller dropdown)
- [ ] Update InvoiceCreateController.java (pass sellers to view)
- **Commit:** `feat: add Seller selection to invoice creation form`
- **Approval checkpoint:** Test invoice creation flow

#### Step 9: Unit Tests
- [ ] Create SellerServiceImplTest.java
- [ ] Create SellerTestDataBuilder.java fixture
- [ ] Run all tests to ensure nothing broke
- **Commit:** `test: add unit tests for SellerService`
- **Approval checkpoint:** Review test coverage

#### Step 10: Integration Testing & Documentation
- [ ] Manual end-to-end testing
- [ ] Update README/documentation if needed
- [ ] Final review of all changes
- **Commit:** `docs: update documentation for Seller feature`
- **Approval checkpoint:** Final review before PR

#### Step 11: Pull Request
- [ ] Create PR: `feature/seller-entity` → `master`
- [ ] Code review
- [ ] Merge after approval

### Phase 2 (Future - when ready to sell SaaS)
- [ ] Design tenant resolution strategy (subdomain vs JWT)
- [ ] Implement Tenant entity and master database
- [ ] Create TenantContext and TenantResolver
- [ ] Implement MultiTenantDataSource routing
- [ ] Build DatabaseProvisionerService
- [ ] Create registration flow with payment
- [ ] Implement tenant onboarding automation
- [ ] Setup wildcard DNS and SSL
- [ ] Migrate existing data to tenant structure
- [ ] Security audit
- [ ] Load testing
- [ ] Launch MVP

---

## Critical Files to Modify (Phase 1)

**New Files:**
- `src/main/java/dev/robgro/timesheet/seller/Seller.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerDto.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerDtoMapper.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerRepository.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerService.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerServiceImpl.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerController.java`
- `src/main/java/dev/robgro/timesheet/seller/SellerViewController.java`
- `src/main/java/dev/robgro/timesheet/seller/OperationResult.java`
- `src/main/resources/templates/sellers/list.html`
- `src/main/resources/templates/sellers/form.html`
- `src/main/resources/db/migration/V4_create_seller_entity.sql`
- `src/test/java/dev/robgro/timesheet/service/SellerServiceImplTest.java`
- `src/test/java/dev/robgro/timesheet/fixtures/SellerTestDataBuilder.java`

**Modified Files:**
- `src/main/java/dev/robgro/timesheet/invoice/Invoice.java` (add seller field)
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceDto.java` (add sellerId)
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceDtoMapper.java` (map seller)
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceCreationServiceImpl.java` (add sellerId param)
- `src/main/java/dev/robgro/timesheet/invoice/PdfGenerator.java` (use dynamic seller)
- `src/main/java/dev/robgro/timesheet/invoice/InvoiceDocumentServiceImpl.java` (remove InvoiceSeller)
- `src/main/java/dev/robgro/timesheet/security/SecurityConfig.java` (add seller endpoints)
- `src/main/resources/templates/invoices/create.html` (add seller dropdown)
- `src/main/resources/templates/head.html` (add Sellers navigation)

**Deleted Files:**
- `src/main/java/dev/robgro/timesheet/config/InvoiceSeller.java`

---

## Risk Assessment

**Phase 1 Risks:** ⚠️ LOW
- Straightforward CRUD implementation
- Pattern already established with Client
- No breaking changes if done correctly
- Easy rollback with database backup

**Phase 2 Risks:** 🔴 HIGH
- Complex datasource routing
- Potential connection leaks
- Hard to debug tenant-specific issues
- Expensive mistakes (wrong tenant data exposure)
- Requires extensive testing

**Mitigation:**
- Implement Phase 1 first, validate thoroughly
- Hire consultant for Phase 2 architecture review
- Use feature flags for gradual rollout
- Extensive integration testing with multiple tenants
- Security penetration testing before production

---

## Recommendations

1. **Start with Phase 1** - Get Seller entity working perfectly
2. **Use it for 1-2 months** - Validate the data model and UX
3. **Decide on SaaS timing** - When you're ready to invest 5-6 weeks
4. **Consider alternatives** - Multi-tenant libraries (e.g., Hibernate MultiTenancy, Spring Cloud Tenant)
5. **Get architectural review** - Before implementing Phase 2, consult with senior architect

---

## Questions Before Implementation - ALL ANSWERED ✅

All questions have been answered during planning:

1. ✅ **Should we implement Phase 1 now?** → YES
2. ✅ **Are all required seller fields correct (bank, contact, company, tax)?** → YES (VAT/Tax are optional/nullable)
3. ✅ **Should User have `default_seller_id` for convenience?** → YES - will auto-populate when creating timesheets
4. ✅ **Should Timesheet also have `seller_id` or just Invoice?** → YES - REQUIRED for automated CRON invoice generation
5. ✅ **Confirm frontend location for Sellers menu item** → Settings dropdown menu (scalable for future settings)
6. ✅ **Branch strategy** → All work on `feature/seller-entity` branch with incremental commits

---

**Plan prepared by:** Claude Sonnet 4.5
**Review status:** Awaiting user approval
**Estimated implementation time (Phase 1):** 2-3 days
**Complexity:** Medium (Phase 1), Very High (Phase 2)
