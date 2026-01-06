package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.client.ClientDto;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.timesheet.TimesheetDto;

import java.time.LocalDate;
import java.util.List;

public interface InvoiceCreationService {
    InvoiceDto createInvoiceFromTimesheets(ClientDto client, Seller seller, List<TimesheetDto> timesheets, LocalDate issueDate);

    InvoiceDto createInvoice(Long clientId, Long sellerId, LocalDate issueDate, List<Long> timesheetIds);

    InvoiceDto buildInvoicePreview(Long clientId, Long sellerId, LocalDate issueDate, List<Long> timesheetIds);
}
