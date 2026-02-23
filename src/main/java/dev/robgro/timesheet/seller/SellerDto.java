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
    String website,

    boolean active,
    boolean systemDefault
) {}