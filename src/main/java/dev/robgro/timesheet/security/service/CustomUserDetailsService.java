package dev.robgro.timesheet.security.service;

import dev.robgro.timesheet.model.entity.Role;
import dev.robgro.timesheet.model.entity.User;
import dev.robgro.timesheet.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to authenticate user: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found: {}", username);
                    return new UsernameNotFoundException("User not found with username: " + username);
                });

        log.debug("User found: {}, active: {}", user.getUsername(), user.isActive());
        log.debug("User has a valid password hash in the database");

        Set<Role> userRoles = user.getRoles();
        log.debug("User roles (count): {}", userRoles.size());
        userRoles.forEach(role -> log.debug("Role: {}", role.getName()));

        Collection<SimpleGrantedAuthority> authorities = getAuthorities(userRoles);
        log.debug("Authorities (count): {}", authorities.size());
        authorities.forEach(auth -> log.debug("Authority: {}", auth.getAuthority()));

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.isActive())
                .build();

        log.debug("Created UserDetails: {}", userDetails);

        return userDetails;
    }

    private Collection<SimpleGrantedAuthority> getAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
    }
}
