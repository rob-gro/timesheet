package dev.robgro.timesheet.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Getter
public class CustomUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final Integer tokenVersion;
    private final Long sellerId;  // Added for SaaS tenant isolation
    private final Collection<? extends GrantedAuthority> authorities;
    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    public CustomUserPrincipal(
            Long userId,
            String username,
            String password,
            Integer tokenVersion,
            Long sellerId,
            Collection<? extends GrantedAuthority> authorities,
            boolean enabled) {

        this.userId = userId;
        this.username = username;
        this.password = password;
        this.tokenVersion = tokenVersion;
        this.sellerId = sellerId;
        this.authorities = authorities;
        this.enabled = enabled;
        this.accountNonExpired = true;
        this.accountNonLocked = true;
        this.credentialsNonExpired = true;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * CRITICAL for SessionRegistry.getAllSessions() to work!
     * SessionRegistry compares principals using equals().
     * We compare by username (unique identifier).
     *
     * This allows:
     * - sessionRegistry.getAllSessions(user.getUsername(), false) to work with String
     * - sessionRegistry.getAllSessions(principal, false) to work with CustomUserPrincipal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;

        // Support comparison with String (username)
        if (obj instanceof String) {
            return username.equals(obj);
        }

        // Support comparison with other CustomUserPrincipal
        if (obj instanceof CustomUserPrincipal other) {
            return username.equals(other.username);
        }

        return false;
    }

    @Override
    public int hashCode() {
        // Hash based on username only (for SessionRegistry lookups)
        return username != null ? username.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "CustomUserPrincipal{" +
               "username='" + username + '\'' +
               ", userId=" + userId +
               ", tokenVersion=" + tokenVersion +
               '}';
    }
}
