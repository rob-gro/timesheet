package dev.robgro.timesheet.seller;

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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/sellers")
@RequiredArgsConstructor
@Tag(name = "Seller Controller", description = "API endpoints for seller operations")
public class SellerController {

    private final SellerService sellerService;

    @Operation(summary = "Get all sellers")
    @ApiResponse(responseCode = "200", description = "List of sellers retrieved successfully")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SellerDto>> getAllSellers() {
        return ResponseEntity.ok(sellerService.getAllSellers());
    }

    @Operation(summary = "Get seller by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller found"),
            @ApiResponse(responseCode = "404", description = "Seller not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<SellerDto> getSellerById(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.getSellerById(id));
    }

    @Operation(summary = "Search sellers by name")
    @ApiResponse(responseCode = "200", description = "Sellers matching search criteria")
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<SellerDto>> searchSellers(@RequestParam(required = false) String name) {
        return ResponseEntity.ok(sellerService.searchSellersByName(name));
    }

    @Operation(summary = "Create new seller",
            description = "Creates a new seller with provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Seller created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDto> createSeller(@Valid @RequestBody SellerDto sellerDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sellerService.createSeller(sellerDto));
    }

    @Operation(summary = "Update existing seller",
            description = "Updates seller details for a specific seller ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller updated successfully"),
            @ApiResponse(responseCode = "404", description = "Seller not found"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SellerDto> updateSeller(@PathVariable Long id,
                                                    @Valid @RequestBody SellerDto sellerDto) {
        return ResponseEntity.ok(sellerService.updateSeller(id, sellerDto));
    }

    @Operation(summary = "Deactivate seller by ID",
            description = "Soft delete - marks seller as inactive instead of removing from database")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Seller successfully deactivated"),
            @ApiResponse(responseCode = "404", description = "Seller not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OperationResult> deactivateSeller(@PathVariable Long id) {
        return ResponseEntity.ok(sellerService.deactivateSeller(id));
    }
}