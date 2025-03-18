package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceCreationService {
    InvoiceDto createInvoiceFromTimesheets(ClientDto client, List<TimesheetDto> timesheets, LocalDate issueDate);

    InvoiceDto createInvoice(Long clientId, LocalDate issueDate, List<Long> timesheetIds);
}
