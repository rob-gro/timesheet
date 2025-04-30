package dev.robgro.timesheet.invoice;

import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class InvoiceItemDtoMapper implements Function<InvoiceItem, InvoiceItemDto> {
    @Override
    public InvoiceItemDto apply(InvoiceItem invoiceItem) {
        return new InvoiceItemDto(
                invoiceItem.getId(),
                invoiceItem.getServiceDate(),
                invoiceItem.getDescription(),
                invoiceItem.getDuration(),
                invoiceItem.getAmount()
        );
    }
}
