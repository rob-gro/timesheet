package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.TimesheetDto;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetService {

    List<TimesheetDto> getAllTimesheets();

    TimesheetDto getTimesheetById(Long id);

    TimesheetDto createTimesheet(Long clientId, LocalDate serviceDate, double duration);

    TimesheetDto updateTimesheet(Long id, Long clientId, LocalDate serviceDate, double duration);

    void deleteTimesheet(Long id);

    List<TimesheetDto> getTimesheetByClientId(Long clientId);

    public List<TimesheetDto> getMonthlyTimesheets(Long clientId, int year, int month);

    void markAsInvoiced(Long id);
}
