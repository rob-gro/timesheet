package dev.robgro.timesheet.model.dto;

import dev.robgro.timesheet.model.entity.InvoiceItem;
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
