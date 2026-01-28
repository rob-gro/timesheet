package dev.robgro.timesheet.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for CSRF protection
 *
 * Tests verify:
 * 1. Authenticated endpoints require CSRF token
 * 2. Public endpoints work without CSRF
 * 3. /api/track/** is GET-only (enforced)
 */
@SpringBootTest
@AutoConfigureMockMvc
class CsrfProtectionTest {

    @Autowired
    private MockMvc mockMvc;

    // ========================================
    // Negative Tests - CSRF Required
    // ========================================

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenPostTimesheetWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/timesheets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"serviceDate\":\"2026-01-27\",\"duration\":8.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenPutTimesheetWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/v1/timesheets/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"serviceDate\":\"2026-01-27\",\"duration\":8.0}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenDeleteTimesheetWithoutCsrf() throws Exception {
        mockMvc.perform(delete("/api/v1/timesheets/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenPostPaymentWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/timesheets/1/payment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentDate\":\"2026-01-27\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturn403_whenPutInvoiceWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/v1/invoices/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sellerId\":1,\"clientId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn403_whenPostUserWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"email\":\"test@example.com\",\"password\":\"pass123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn403_whenPutUserWithoutCsrf() throws Exception {
        mockMvc.perform(put("/api/v1/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test\",\"email\":\"test@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    // ========================================
    // Positive Tests - CSRF Token Provided
    // ========================================

    @Test
    @WithMockUser(roles = "USER")
    void shouldAcceptRequest_whenPostTimesheetWithCsrf() throws Exception {
        // Note: This will still fail validation (missing client, etc.)
        // but should NOT fail with 403 Forbidden - any other status is acceptable
        mockMvc.perform(post("/api/v1/timesheets")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\":1,\"serviceDate\":\"2026-01-27\",\"duration\":8.0}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Expected any status except 403 Forbidden, but got 403");
                    }
                });
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldAcceptRequest_whenDeleteTimesheetWithCsrf() throws Exception {
        mockMvc.perform(delete("/api/v1/timesheets/999")
                        .with(csrf()))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Expected any status except 403 Forbidden, but got 403");
                    }
                });
    }

    // ========================================
    // Public Endpoints - No CSRF Required
    // ========================================

    @Test
    void shouldAllowPublicEndpoint_forgotPasswordWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Public endpoint should not return 403 Forbidden, but got 403");
                    }
                });
    }

    @Test
    void shouldAllowPublicEndpoint_resetPasswordWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"test-token\",\"newPassword\":\"newpass123\"}"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("Public endpoint should not return 403 Forbidden, but got 403");
                    }
                });
    }

    // ========================================
    // /api/track/** - GET-only Enforcement
    // ========================================

    @Test
    void shouldAllow_getRequestToEmailTracking() throws Exception {
        mockMvc.perform(get("/api/track/some-token"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == 403) {
                        throw new AssertionError("GET request to /api/track/** should not return 403 Forbidden, but got 403");
                    }
                });
    }

    @Test
    void shouldDeny_postRequestToEmailTracking() throws Exception {
        mockMvc.perform(post("/api/track/some-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden()); // Must be Forbidden (enforced GET-only)
    }

    @Test
    void shouldDeny_putRequestToEmailTracking() throws Exception {
        mockMvc.perform(put("/api/track/some-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden()); // Must be Forbidden (enforced GET-only)
    }

    @Test
    void shouldDeny_deleteRequestToEmailTracking() throws Exception {
        mockMvc.perform(delete("/api/track/some-token"))
                .andExpect(status().isForbidden()); // Must be Forbidden (enforced GET-only)
    }

    @Test
    void shouldDeny_patchRequestToEmailTracking() throws Exception {
        mockMvc.perform(patch("/api/track/some-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden()); // Must be Forbidden (enforced GET-only)
    }
}
