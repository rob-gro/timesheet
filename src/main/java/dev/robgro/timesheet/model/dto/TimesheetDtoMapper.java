package dev.robgro.timesheet.model.dto;

import dev.robgro.timesheet.model.entity.Timesheet;
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
                timesheet.isInvoice(),
                timesheet.getClient().getId(),
                timesheet.getClient().getHourlyRate()
        );
    }
}
