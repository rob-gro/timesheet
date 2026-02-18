package dev.robgro.timesheet.invoice;

import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * Mapper for converting InvoiceNumberingScheme entity to DTO.
 */
@Service
public class InvoiceNumberingSchemeDtoMapper implements Function<InvoiceNumberingScheme, InvoiceNumberingSchemeDto> {

    @Override
    public InvoiceNumberingSchemeDto apply(InvoiceNumberingScheme scheme) {
        return new InvoiceNumberingSchemeDto(
            scheme.getId(),
            scheme.getSeller().getId(),
            scheme.getSeller().getName(),
            scheme.getTemplate(),
            scheme.getResetPeriod(),
            scheme.getEffectiveFrom(),
            scheme.getVersion(),
            scheme.getStatus(),
            scheme.getCreatedAt(),
            scheme.getUpdatedAt(),
            scheme.getCreatedBy() != null ? scheme.getCreatedBy().getUsername() : null
        );
    }
}
