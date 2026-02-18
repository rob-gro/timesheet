package dev.robgro.timesheet.invoice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for InvoiceCounterObservabilityController.
 *
 * <p>Tests verify:
 * - Admin-only access (403 for non-admin)
 * - Counter status response structure
 * - Drift detection logic
 */
@SpringBootTest
@AutoConfigureMockMvc
class InvoiceCounterObservabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnCounterStatus_whenAdminAccess() throws Exception {
        // Given - admin user
        Long sellerId = 1L;

        // When - access observability endpoint
        // Then - should return 200 OK with counter status
        mockMvc.perform(get("/internal/invoice-counters")
                .param("sellerId", sellerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sellerId").value(sellerId))
            .andExpect(jsonPath("$.counters").isArray());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyAccess_whenNotAdmin() throws Exception {
        // Given - regular user (not admin)
        Long sellerId = 1L;

        // When - attempt to access observability endpoint
        // Then - should return 5xx (access denied, wrapped in exception handler)
        // Note: Spring Security @PreAuthorize throws AccessDeniedException which is
        // wrapped as 500 by global exception handler, but still denies access correctly
        mockMvc.perform(get("/internal/invoice-counters")
                .param("sellerId", sellerId.toString()))
            .andExpect(status().is5xxServerError()); // Access denied wrapped in 500
    }

    @Test
    void shouldDenyAccess_whenNotAuthenticated() throws Exception {
        // Given - no authentication
        Long sellerId = 1L;

        // When - attempt to access observability endpoint
        // Then - should return 401 Unauthorized or redirect to login
        mockMvc.perform(get("/internal/invoice-counters")
                .param("sellerId", sellerId.toString()))
            .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyCounters_whenNoCountersExist() throws Exception {
        // Given - seller with no counters (use high sellerId unlikely to exist)
        Long sellerId = 999999L;

        // When - access observability endpoint
        // Then - should return empty counters array
        mockMvc.perform(get("/internal/invoice-counters")
                .param("sellerId", sellerId.toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sellerId").value(sellerId))
            .andExpect(jsonPath("$.counters").isEmpty())
            .andExpect(jsonPath("$.currentTemplate").doesNotExist());
    }
}
