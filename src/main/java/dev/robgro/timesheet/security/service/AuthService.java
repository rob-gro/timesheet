package dev.robgro.timesheet.security.service;

import dev.robgro.timesheet.security.dto.JwtResponse;
import dev.robgro.timesheet.security.dto.LoginRequest;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);

    boolean validateToken(String token);
}
