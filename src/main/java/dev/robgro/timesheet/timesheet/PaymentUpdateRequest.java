package dev.robgro.timesheet.timesheet;

import java.time.LocalDate;

public record PaymentUpdateRequest(
        LocalDate paymentDate
) {
}
