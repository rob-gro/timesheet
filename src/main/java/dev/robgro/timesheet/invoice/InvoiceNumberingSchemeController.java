package dev.robgro.timesheet.invoice;

import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for invoice numbering schemes.
 * Provides endpoints for managing configurable invoice numbering.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/invoice-numbering-schemes")
@RequiredArgsConstructor
@Tag(name = "Invoice Numbering Schemes", description = "API endpoints for managing invoice numbering configuration")
public class InvoiceNumberingSchemeController {

    private final InvoiceNumberingSchemeService schemeService;
    private final UserService userService;

    @Operation(summary = "Get all numbering schemes for current seller")
    @ApiResponse(responseCode = "200", description = "List of schemes retrieved successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<InvoiceNumberingSchemeDto>> getAllSchemes() {
        return ResponseEntity.ok(schemeService.getAllSchemes());
    }

    @Operation(summary = "Get active numbering schemes for current seller")
    @ApiResponse(responseCode = "200", description = "List of active schemes retrieved successfully")
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<InvoiceNumberingSchemeDto>> getActiveSchemes() {
        return ResponseEntity.ok(schemeService.getActiveSchemes());
    }

    @Operation(summary = "Create new numbering scheme",
            description = "Creates a new invoice numbering scheme for current seller")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Scheme created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<InvoiceNumberingSchemeDto> createScheme(
            @Valid @RequestBody CreateSchemeRequest request) {
        // createdBy is automatically set by Spring Data JPA Auditing
        InvoiceNumberingSchemeDto created = schemeService.createScheme(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Archive numbering scheme by ID",
            description = "Archives a numbering scheme (makes it historical)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Scheme archived successfully"),
            @ApiResponse(responseCode = "404", description = "Scheme not found"),
            @ApiResponse(responseCode = "403", description = "Not authorized to modify this scheme")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> archiveScheme(@PathVariable Long id) {
        schemeService.archiveScheme(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Preview invoice number template",
            description = "Generate example invoice number from template for preview")
    @ApiResponse(responseCode = "200", description = "Preview generated successfully")
    @PostMapping("/preview")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, String>> previewTemplate(@RequestBody Map<String, String> request) {
        String template = request.get("template");
        String preview = schemeService.previewTemplate(template);
        return ResponseEntity.ok(Map.of("preview", preview));
    }
}
