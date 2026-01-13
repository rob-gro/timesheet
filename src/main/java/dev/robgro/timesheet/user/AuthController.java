package dev.robgro.timesheet.user;

import dev.robgro.timesheet.security.JwtResponse;
import dev.robgro.timesheet.security.LoginRequest;
import dev.robgro.timesheet.security.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Request to authenticate user: {}", loginRequest.getUsername());

        JwtResponse jwtResponse = authService.login(loginRequest);

        return ResponseEntity.ok(jwtResponse);
    }

    @PostMapping("/change-password-required")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> changePasswordRequired(
            @Valid @RequestBody PasswordChangeRequiredDto passwordDto,
            Principal principal,
            HttpServletRequest request) {

        log.debug("Password change request for user: {}", principal.getName());

        try {
            userService.changePasswordAfterReset(principal.getName(), passwordDto);

            // Invalidate session
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();

            log.info("Password changed successfully for user: {}", principal.getName());

            return ResponseEntity.ok(Map.of(
                    "message", "Password changed successfully. Please login with your new password."
            ));

        } catch (IllegalArgumentException | IllegalStateException e) {
            log.warn("Password change failed for user {}: {}", principal.getName(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
