package dev.robgro.timesheet.timesheet;

import org.springframework.stereotype.Service;

import java.util.function.Function;

@Service
public class TimesheetDtoMapper implements Function<Timesheet, TimesheetDto> {
    @Override
    public TimesheetDto apply(Timesheet timesheet) {
        return new TimesheetDto(
                timesheet.getId(),
                timesheet.getClient().getClientName(),
                timesheet.getServiceDate(),
                timesheet.getDuration(),
                timesheet.isInvoiced(),
                timesheet.getClient().getId(),
                timesheet.getEffectiveHourlyRate(),
                timesheet.getInvoice() == null ? null : timesheet.getInvoice().getInvoiceNumber(),
                timesheet.getPaymentDate(),
                timesheet.getValue()
        );
    }
}
