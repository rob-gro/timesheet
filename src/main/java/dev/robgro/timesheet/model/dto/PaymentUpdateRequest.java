package dev.robgro.timesheet.model.dto;

import java.time.LocalDate;

public record PaymentUpdateRequest(
        LocalDate paymentDate
) {
}
