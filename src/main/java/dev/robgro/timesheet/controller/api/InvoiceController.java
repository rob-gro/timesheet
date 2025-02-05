package dev.robgro.timesheet.controller.api;

import dev.robgro.timesheet.model.dto.CreateInvoiceRequest;
import dev.robgro.timesheet.model.dto.DeleteInvoiceRequest;
import dev.robgro.timesheet.model.dto.InvoiceDto;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.service.BillingService;
import dev.robgro.timesheet.service.InvoiceService;
import dev.robgro.timesheet.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice Controller", description = "API endpoints for invoice and timesheet reporting operations")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final TimesheetService timesheetService;
    private final BillingService billingService;


    @Operation(summary = "Create invoice for selected timesheets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Input invalid data"),
            @ApiResponse(responseCode = "404", description = "Client or timesheets not found")
    })
    @GetMapping("/invoices")
    public ResponseEntity<InvoiceDto> createInvoice(
            @PathVariable Long clientId,
            @RequestBody CreateInvoiceRequest request) {
        InvoiceDto invoice = billingService.createInvoice(
                clientId,
                request.issueDate(),
                request.timesheetIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @Operation(summary = "Create monthly invoice automatically")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Monthly invoice created successfully"),
            @ApiResponse(responseCode = "400", description = "Input invalid data or no timesheets to invoice"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PostMapping("/monthly")
    public ResponseEntity<InvoiceDto> createMonthlyInvoice(
            @PathVariable Long clientId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billingService.createMonthlyInvoice(clientId, year, month));
    }

    @Operation(summary = "Get all timesheets for a client")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of timesheets retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/timesheets/{clientId}")
    public ResponseEntity<List<TimesheetDto>> getTimesheetsByClientId(
            @PathVariable Long clientId) {
        return ResponseEntity.ok(timesheetService.getTimesheetByClientId(clientId));
    }

    @Operation(summary = "Get client's timesheets for specific month")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of timesheets retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid date parameters"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/monthly/timesheets")
    public ResponseEntity<List<TimesheetDto>> getMonthlyTimesheets(
            @PathVariable Long clientId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(timesheetService.getMonthlyTimesheets(clientId, year, month));
    }

    @Operation(summary = "Get client's invoices for specific month")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/monthly")
    public ResponseEntity<List<InvoiceDto>> getMonthlyInvoices(
            @RequestParam Long clientId,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(invoiceService.getMonthlyInvoices(clientId, year, month));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> getInvoicePdf(@PathVariable Long id) {
        InvoiceDto invoice = invoiceService.getInvoiceById(id);
        if (invoice.pdfPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF not found for this invoice");
        }

        byte[] pdfContent = invoiceService.getInvoicePdfContent(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + invoice.invoiceNumber() + ".pdf\"")
                .body(pdfContent);
    }

    @Operation(summary = "Get client's invoices for specific year")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of invoices retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/yearly")
    public ResponseEntity<List<InvoiceDto>> getYearlyInvoices(
            @PathVariable Long clientId,
            @RequestParam int year) {
        return ResponseEntity.ok(invoiceService.getYearlyInvoices(clientId, year));
    }

    @Operation(summary = "Delete invoice and optionally its timesheets")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Invoice deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Invoice not found")
    })
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id,
            @RequestBody DeleteInvoiceRequest request) {
        invoiceService.deleteInvoice(id, request.deleteTimesheets(), request.detachFromClient());
        return ResponseEntity.noContent().build();
    }
}
