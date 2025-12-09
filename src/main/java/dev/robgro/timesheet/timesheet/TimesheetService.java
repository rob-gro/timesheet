package dev.robgro.timesheet.timesheet;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TimesheetService {
    TimesheetDto createTimesheet(Long clientId, LocalDate serviceDate, double duration, Boolean isPaidAlready);

    List<TimesheetDto> getAllTimesheets();

    TimesheetDto getTimesheetById(Long id);

    List<TimesheetDto> getTimesheetByClientId(Long clientId);

    public List<TimesheetDto> getMonthlyTimesheets(Long clientId, int year, int month);

    List<TimesheetDto> getUnbilledTimesheets();

    List<TimesheetDto> getUnbilledTimesheetsByClientId(Long clientId);

    List<TimesheetDto> getTimesheetsByFilters(Long clientId, String paymentStatus);

    List<TimesheetDto> searchAndSortTimesheets(Long clientId, String sortBy, String sortDir);

    TimesheetDto updateTimesheet(Long id, Long clientId, LocalDate serviceDate, double duration, Boolean isPaidAlready);

    void deleteTimesheet(Long id);

    void markAsInvoiced(Long id);

    void updateInvoiceFlag(Long id, boolean isInvoiced);

    void detachFromInvoice(Long id);

    List<TimesheetDto> getTimesheetsByClientAndInvoiceStatus(Long clientId, boolean invoiced);

    Page<TimesheetDto> getFilteredAndPaginatedTimesheets(Long clientId, String paymentStatus,
                                                         String sortBy, String sortDir, int page, int size);

    Page<TimesheetDto> getAllTimesheetsPageable(Pageable pageable);

    Page<TimesheetDto> getTimesheetsByClientIdPageable(Long clientId, Pageable pageable);

    Page<TimesheetDto> getAllTimesheetsSortedByInvoiceNumber(Long clientId, Pageable pageable);

    Page<TimesheetDto> getAllTimesheetsPageable(Long clientId, Pageable pageable);

    void updatePaymentDate(Long id, LocalDate paymentDate);

    TimesheetDto createEmptyTimesheetDto();
}
