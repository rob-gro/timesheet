package dev.robgro.timesheet.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                   @NonNull HttpServletResponse response,
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = getTokenFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.getUsername(jwt);
                Integer tokenVersion = tokenProvider.getTokenVersionFromToken(jwt);

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Verify token version matches current user's version
                if (userDetails instanceof CustomUserPrincipal principal) {
                    if (!principal.getTokenVersion().equals(tokenVersion)) {
                        log.warn("Token version mismatch for user {}: token={}, current={}, uri={}",
                                username, tokenVersion, principal.getTokenVersion(), request.getRequestURI());

                        // Clear security context FIRST (before any branching)
                        SecurityContextHolder.clearContext();

                        // For API requests, return 401 JSON (not 403!)
                        // 401 = unauthenticated (token invalid)
                        // 403 = authenticated but unauthorized
                        if (isApiRequest(request)) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");

                            Map<String, String> error = Map.of(
                                    "error", "Token invalidated",
                                    "message", "Your token has been invalidated. Please login again."
                            );

                            response.getWriter().write(objectMapper.writeValueAsString(error));
                            return; // Stop filter chain - do NOT continue as anonymous
                        }

                        // For web UI requests, continue as anonymous → will redirect to login
                        filterChain.doFilter(request, response);
                        return;
                    }
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set authentication for user: {}", username);
            }

        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");

        // Primary heuristic: URI starts with /api/
        if (uri.startsWith("/api/")) {
            return true;
        }

        // Secondary heuristic: Accept header contains application/json
        return accept != null && accept.contains("application/json");
    }
}
