package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.TimesheetDto;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetService {
    TimesheetDto createTimesheet(Long clientId, LocalDate serviceDate, double duration);

    List<TimesheetDto> getAllTimesheets();

    TimesheetDto getTimesheetById(Long id);

    List<TimesheetDto> getTimesheetByClientId(Long clientId);

    public List<TimesheetDto> getMonthlyTimesheets(Long clientId, int year, int month);

    List<TimesheetDto> getUnbilledTimesheets();

    List<TimesheetDto> getUnbilledTimesheetsByClientId(Long clientId);

    TimesheetDto updateTimesheet(Long id, Long clientId, LocalDate serviceDate, double duration);

    void deleteTimesheet(Long id);

    void markAsInvoiced(Long id);

    void updateInvoiceFlag(Long id, boolean isInvoiced);

    void detachFromInvoice(Long id);


    List<TimesheetDto> getTimesheetsByClientAndInvoiceStatus(Long clientId, boolean isInvoice);
}
