//package dev.robgro.timesheet.controller.api;
//
//import dev.robgro.timesheet.model.dto.InvoiceDto;
//import dev.robgro.timesheet.model.dto.InvoiceUpdateRequest;
//import dev.robgro.timesheet.service.BillingService;
//import dev.robgro.timesheet.service.InvoiceService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.responses.ApiResponses;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@RestController
//@RequestMapping("/api/v1/invoices")
//@RequiredArgsConstructor
//@Tag(name = "Invoice Management", description = "API endpoints for general invoice operations")
//public class InvoiceManagementController {
//
//    private final InvoiceService invoiceService;
//    private final BillingService billingService;
//
//    @Operation(summary = "Get all invoices")
//    @ApiResponse(responseCode = "200", description = "List of all invoices retrieved successfully")
//    @GetMapping
//    public ResponseEntity<List<InvoiceDto>> getAllInvoices() {
//        return ResponseEntity.ok(invoiceService.getAllInvoices());
//    }
//
//    @Operation(summary = "Get invoice by ID")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Invoice found"),
//            @ApiResponse(responseCode = "404", description = "Invoice not found")
//    })
//    @GetMapping("/{id}")
//    public ResponseEntity<InvoiceDto> getInvoiceById(@PathVariable long id) {
//        return ResponseEntity.ok(invoiceService.getInvoiceById(id));
//    }
//
//    @Operation(summary = "Find invoice by number")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Invoice found"),
//            @ApiResponse(responseCode = "404", description = "Invoice not found")
//    })
//    @GetMapping("/number/{invoiceNumber}")
//    public ResponseEntity<InvoiceDto> findByInvoiceNumber(@PathVariable String invoiceNumber) {
//        return ResponseEntity.ok(invoiceService.findByInvoiceNumber(invoiceNumber)
//                .orElseThrow(() -> new ResponseStatusException(
//                        HttpStatus.NOT_FOUND, "Invoice not found with number: " + invoiceNumber)));
//    }
//
//    @Operation(summary = "Generate monthly invoices for all clients")
//    @ApiResponse(responseCode = "201", description = "Monthly invoices generated successfully")
//    @PostMapping("/monthly/generate")
//    public ResponseEntity<List<InvoiceDto>> generateMonthlyInvoices(
//            @RequestParam int year,
//            @RequestParam int month) {
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(billingService.generateMonthlyInvoices(year, month));
//    }
//
//    @Operation(summary = "Update existing invoice")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "Invoice updated successfully"),
//            @ApiResponse(responseCode = "404", description = "Invoice not found"),
//            @ApiResponse(responseCode = "400", description = "Invalid input data")
//    })
//    @PutMapping("/{id}")
//    public ResponseEntity<InvoiceDto> updateInvoice(
//            @PathVariable Long id,
//            @RequestBody InvoiceUpdateRequest request) {
//        return ResponseEntity.ok(invoiceService.updateInvoice(id, request));
//    }
//}
