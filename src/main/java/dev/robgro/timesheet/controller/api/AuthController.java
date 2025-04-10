package dev.robgro.timesheet.controller.api;

import dev.robgro.timesheet.security.dto.JwtResponse;
import dev.robgro.timesheet.security.dto.LoginRequest;
import dev.robgro.timesheet.security.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        log.debug("Request to authenticate user: {}", loginRequest.getUsername());

        JwtResponse jwtResponse = authService.login(loginRequest);

        return ResponseEntity.ok(jwtResponse);
    }
}
