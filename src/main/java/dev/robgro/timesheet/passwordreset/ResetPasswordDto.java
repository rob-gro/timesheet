package dev.robgro.timesheet.passwordreset;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordDto(
    @NotBlank(message = "Token is required")
    String token,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword,

    @NotBlank(message = "Password confirmation is required")
    String confirmPassword
) {
    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }
}
