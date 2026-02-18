package dev.robgro.timesheet.security;

import dev.robgro.timesheet.exception.BusinessRuleViolationException;
import dev.robgro.timesheet.user.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    private SecurityUtils() {
        throw new BusinessRuleViolationException("Utility class cannot be instantiated");
    }

    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                !auth.getPrincipal().equals("anonymousUser");
    }

    /**
     * Get current user's seller ID from security context.
     * Used for tenant isolation in multi-tenant invoice numbering.
     *
     * @return Seller ID of current user, or null if not authenticated or no seller assigned
     */
    public static Long getCurrentSellerId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserPrincipal customUserPrincipal) {
            return customUserPrincipal.getSellerId();
        }

        return null;
    }
}
