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
                timesheet.getServiceDate(),
                timesheet.getDuration(),
                timesheet.isInvoice(),
                timesheet.getClient().getId()
        );
    }
}
