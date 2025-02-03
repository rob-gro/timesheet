package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.TimesheetDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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

    List<TimesheetDto> searchAndSortTimesheets(Long clientId, String sortBy, String sortDir);

    TimesheetDto updateTimesheet(Long id, Long clientId, LocalDate serviceDate, double duration);

    void deleteTimesheet(Long id);

    void markAsInvoiced(Long id);

    void updateInvoiceFlag(Long id, boolean isInvoiced);

    void detachFromInvoice(Long id);

    List<TimesheetDto> getTimesheetsByClientAndInvoiceStatus(Long clientId, boolean invoiced);


    Page<TimesheetDto> getAllTimesheetsPageable(Pageable pageable);
    Page<TimesheetDto> getTimesheetsByClientIdPageable(Long clientId, Pageable pageable);

    Page<TimesheetDto> getAllTimesheetsSortedByInvoiceNumber(Long clientId, Pageable pageable);

    Page<TimesheetDto> getAllTimesheetsPageable(Long clientId, Pageable pageable);

//    Page<TimesheetDto> getAllTimesheets(Pageable pageable);
//    Page<TimesheetDto> getTimesheetsByClientId(Long clientId, Pageable pageable);
//    Page<TimesheetDto> getTimesheetsByClientAndInvoiceStatus(Long clientId, boolean invoiced, Pageable pageable);
//    Page<TimesheetDto> searchAndSortTimesheets(Long clientId, String sortBy, String sortDir, Pageable pageable);
}
