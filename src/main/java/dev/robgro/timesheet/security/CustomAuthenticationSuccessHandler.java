package dev.robgro.timesheet.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                       HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {

        if (authentication.getPrincipal() instanceof CustomUserPrincipal principal) {

            // Check if password change required
            if (principal.isRequiresPasswordChange()) {
                log.info("User {} requires password change, redirecting", principal.getUsername());

                // Check if temporary password expired
                // Note: We don't have tempPasswordExpiresAt in principal (not needed for every request)
                // Expiry check will happen in the change-password endpoint

                getRedirectStrategy().sendRedirect(request, response, "/change-password-required");
                return;
            }
        }

        // Default success URL
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
