package dev.robgro.timesheet.controller.api;

import dev.robgro.timesheet.model.dto.ClientDto;
import dev.robgro.timesheet.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Client Controller", description = "API endpoints for client operations")
public class ClientController {

    private final ClientService clientService;

    @Operation(summary = "Get all clients")
    @ApiResponse(responseCode = "200", description = "List of clients retrieved successfully")
    @GetMapping
    public ResponseEntity<List<ClientDto>> getAllClients() {
        return ResponseEntity.ok(clientService.getAllClients());
    }

    @Operation(summary = "Get client by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client found"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ClientDto> getClientById(@PathVariable Long id) {
        return ResponseEntity.ok(clientService.getClientById(id));
    }

    @PostMapping
    public ResponseEntity<ClientDto> createClient(@RequestBody ClientDto clientDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clientService.createClient(clientDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientDto> updateClient(@PathVariable Long id,
                                                  @RequestBody ClientDto clientDto) {
        return ResponseEntity.ok(clientService.updateClient(id, clientDto));
    }

    @Operation(summary = "Delete client by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Client successfully deleted"),
            @ApiResponse(responseCode = "404", description = "Client not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        try {
            clientService.deleteClient(id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            log.error("Error deleting client with id: {}", id, e);
            return ResponseEntity.status(e.getStatusCode()).build();
        } catch (Exception e) {
            log.error("Unexpected error while deleting client with id: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
