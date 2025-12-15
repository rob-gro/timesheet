package dev.robgro.timesheet.invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDto(
        Long id,
        Long clientId,
        String clientName,
        String invoiceNumber,
        LocalDate issueDate,
        BigDecimal totalAmount,
        String pdfPath,
        List<InvoiceItemDto> itemsList,
        LocalDateTime pdfGeneratedAt,
        LocalDateTime emailSentAt,
        LocalDateTime emailOpenedAt,
        Integer emailOpenCount,
        LocalDateTime lastEmailOpenedAt,
        String emailStatus
) {
}
