package dev.robgro.timesheet.invoice;

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
                invoice.getClient().getId(),
                invoice.getClient().getClientName(),
                invoice.getInvoiceNumber(),
                invoice.getIssueDate(),
                invoice.getTotalAmount(),
                invoice.getPdfPath(),
                invoice.getItemsList().stream()
                        .map(invoiceItemDtoMapper)
                        .collect(Collectors.toList()),
                invoice.getPdfGeneratedAt(),
                invoice.getEmailSentAt(),
                invoice.getEmailOpenedAt(),
                invoice.getEmailOpenCount(),
                invoice.getLastEmailOpenedAt(),
                invoice.getEmailStatus()
        );
    }
}
