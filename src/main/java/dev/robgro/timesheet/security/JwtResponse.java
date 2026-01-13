package dev.robgro.timesheet.security;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {

    private String token;
    private String type = "Bearer";
    private boolean requiresPasswordChange;

    public JwtResponse(String token) {
        this.token = token;
        this.requiresPasswordChange = false;
    }

    public JwtResponse(String token, boolean requiresPasswordChange) {
        this.token = token;
        this.requiresPasswordChange = requiresPasswordChange;
    }
}
