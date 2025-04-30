package dev.robgro.timesheet.security;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);

    boolean validateToken(String token);
}
