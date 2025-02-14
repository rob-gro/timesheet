package dev.robgro.timesheet.controller.api;

import dev.robgro.timesheet.model.dto.CreateTimesheetRequest;
import dev.robgro.timesheet.model.dto.PaymentUpdateRequest;
import dev.robgro.timesheet.model.dto.TimesheetDto;
import dev.robgro.timesheet.model.dto.UpdateTimesheetRequest;
import dev.robgro.timesheet.service.TimesheetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/timesheets")
@RequiredArgsConstructor
@Tag(name = "Timesheet Controller", description = "API endpoints for timesheet operations")
public class TimesheetController {

    private final TimesheetService timesheetService;

    @Operation(summary = "Create new timesheet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Timesheet created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @PostMapping
    public ResponseEntity<TimesheetDto> createTimesheet(
            @RequestBody CreateTimesheetRequest request) {
        TimesheetDto timesheet = timesheetService.createTimesheet(
                request.clientId(),
                request.serviceDate(),
                request.duration());
        return ResponseEntity.status(HttpStatus.CREATED).body(timesheet);
    }

    @Operation(summary = "Get all timesheets")
    @ApiResponse(responseCode = "200", description = "List of all timesheets retrieved successfully")
    @GetMapping("/all")
    public ResponseEntity<List<TimesheetDto>> getAllTimesheets() {
        return ResponseEntity.ok(timesheetService.getAllTimesheets());
    }

    @Operation(summary = "Update existing timesheet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timesheet updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Timesheet not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TimesheetDto> updateTimesheet(
            @PathVariable Long id,
            @RequestBody UpdateTimesheetRequest request) {
        TimesheetDto timesheet = timesheetService.updateTimesheet(
                id,
                request.clientId(),
                request.serviceDate(),
                request.duration());
        return ResponseEntity.status(HttpStatus.OK).body(timesheet);
    }

    @Operation(summary = "Delete timesheet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Timesheet deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Timesheet not found"),
            @ApiResponse(responseCode = "400", description = "Cannot delete timesheet attached to invoice")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTimesheet(@PathVariable Long id) {
        timesheetService.deleteTimesheet(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Detach timesheet from invoice")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timesheet detached successfully"),
            @ApiResponse(responseCode = "404", description = "Timesheet not found")
    })
    @PostMapping("/{id}/detach")
    public ResponseEntity<Void> detachFromInvoice(@PathVariable Long id) {
        timesheetService.detachFromInvoice(id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get timesheets by client",
            description = "Retrieve all timesheets for specific client with optional invoice status filter")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Timesheets retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<TimesheetDto>> getTimesheetsByClient(
            @PathVariable Long clientId,
            @RequestParam(required = false) Boolean invoiced) {

        if (invoiced != null) {
            return ResponseEntity.ok(timesheetService.getTimesheetsByClientAndInvoiceStatus(clientId, invoiced));
        }
        return ResponseEntity.ok(timesheetService.getTimesheetByClientId(clientId));
    }

    @Operation(summary = "Update timesheet payment status",
            description = "Updates the payment date for a specific timesheet")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Timesheet not found")
    })
    @PostMapping("/{id}/payment")
    public ResponseEntity<Void> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody PaymentUpdateRequest request) {
        timesheetService.updatePaymentDate(id, request.paymentDate());
        return ResponseEntity.ok().build();
    }
}
