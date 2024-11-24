package dev.robgro.timesheet.model.dto;

import dev.robgro.timesheet.model.entity.Invoice;
import org.springframework.stereotype.Service;

import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceDtoMapper implements Function<Invoice, InvoiceDto> {

    private final InvoiceItemDtoMapper invoiceItemDtoMapper;

    public InvoiceDtoMapper(InvoiceItemDtoMapper invoiceItemDtoMapper) {
        this.invoiceItemDtoMapper = invoiceItemDtoMapper;
    }

    @Override
    public InvoiceDto apply(Invoice invoice) {
        return new InvoiceDto(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getTotalAmount(),
                invoice.getClient().getId(),
                invoice.getItemsList().stream()
                        .map(invoiceItemDtoMapper)
                        .collect(Collectors.toList())
        );
    }
}
