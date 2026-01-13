package dev.robgro.timesheet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Skip if not authenticated or anonymous
        if (auth == null || auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check if principal requires password change
        if (auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            if (principal.isRequiresPasswordChange()) {
                String uri = request.getRequestURI();

                // Allow specific endpoints
                if (isAllowedForPasswordChange(uri)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // Block access - redirect or return 403
                handlePasswordChangeRequired(request, response, uri);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedForPasswordChange(String uri) {
        return uri.equals("/change-password-required")
                || uri.equals("/api/v1/auth/change-password-required")
                || uri.equals("/api/auth/change-password-required")
                || uri.equals("/logout")
                || uri.startsWith("/css/")
                || uri.startsWith("/js/")
                || uri.startsWith("/images/")
                || uri.startsWith("/webjars/")
                || uri.equals("/favicon.ico")
                || uri.startsWith("/error");
    }

    private void handlePasswordChangeRequired(HttpServletRequest request,
                                              HttpServletResponse response,
                                              String uri) throws IOException {

        // API request (JSON response)
        if (isApiRequest(request, uri)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");

            Map<String, String> error = Map.of(
                    "error", "Password change required",
                    "message", "You must change your password before accessing this resource"
            );

            response.getWriter().write(objectMapper.writeValueAsString(error));
            log.warn("Blocked API request to {} - password change required", uri);
        } else {
            // Web request (redirect)
            response.sendRedirect("/change-password-required");
            log.warn("Redirected request to {} - password change required", uri);
        }
    }

    private boolean isApiRequest(HttpServletRequest request, String uri) {
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/")
                || (accept != null && accept.contains("application/json"));
    }
}
