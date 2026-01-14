package dev.robgro.timesheet.passwordreset;

import dev.robgro.timesheet.exception.InvalidTokenException;
import dev.robgro.timesheet.exception.TokenAlreadyUsedException;
import dev.robgro.timesheet.exception.TokenExpiredException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * MVC controller for password reset views.
 * Renders Thymeleaf templates for forgot password and reset password forms.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class PasswordResetViewController {

    private final PasswordResetTokenService tokenService;

    /**
     * Show forgot password form.
     *
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "forgot-password";
    }

    /**
     * Show reset password form (with token validation).
     *
     * CRITICAL: This is a GET request - MUST NOT mutate state.
     * - validateToken() is READ-ONLY (no DB writes, no counters)
     * - No usedAt updates, no token consumption
     * - Token consumption happens ONLY in POST /api/auth/reset-password
     *
     * @param token Reset token from URL parameter
     * @param model Thymeleaf model
     * @return Template name
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm(
            @RequestParam String token,
            Model model) {

        try {
            // Validate token (read-only - no state mutation)
            PasswordResetToken resetToken = tokenService.validateToken(token);

            model.addAttribute("token", token);
            model.addAttribute("valid", true);
            model.addAttribute("expiresAt", resetToken.getExpiresAt());

        } catch (InvalidTokenException | TokenExpiredException | TokenAlreadyUsedException e) {
            model.addAttribute("valid", false);
            model.addAttribute("error", e.getMessage());
        }

        return "reset-password";
    }
}
