package dev.robgro.timesheet.scheduler;

import dev.robgro.timesheet.invoice.InvoiceDto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InvoiceProcessingResult {
    private InvoiceDto invoice;
    private boolean success;
    private String errorMessage;
    private Exception exception;

    public static InvoiceProcessingResult success(InvoiceDto invoice) {
        return InvoiceProcessingResult.builder()
                .invoice(invoice)
                .success(true)
                .build();
    }

    public static InvoiceProcessingResult failure(InvoiceDto invoice, Exception e) {
        return InvoiceProcessingResult.builder()
                .invoice(invoice)
                .success(false)
                .errorMessage(e.getMessage())
                .exception(e)
                .build();
    }
}
