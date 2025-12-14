package dev.robgro.timesheet.security;

import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.role.RoleName;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Role adminRole;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // Create test roles
        adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ROLE_ADMIN);

        userRole = new Role();
        userRole.setId(2L);
        userRole.setName(RoleName.ROLE_USER);

        // Create test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("$2a$10$encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setActive(true);
    }

    @Test
    void shouldLoadUserByUsername_whenUserExists() {
        // given
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$10$encodedPassword");
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(userRepository).findByUsername("testuser");
    }

    @Test
    void shouldThrowUsernameNotFoundException_whenUserNotFound() {
        // given
        when(userRepository.findByUsername("nonexistent"))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with username: nonexistent");

        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    void shouldMapMultipleRolesToAuthorities() {
        // given
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        roles.add(userRole);
        testUser.setRoles(roles);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_USER");
    }

    @Test
    void shouldDisableUser_whenUserIsInactive() {
        // given
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
        testUser.setActive(false);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.isEnabled()).isFalse();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    void shouldEnableUser_whenUserIsActive() {
        // given
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);
        testUser.setActive(true);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.isEnabled()).isTrue();
    }

    @Test
    void shouldMapSingleRole_correctly() {
        // given
        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        testUser.setRoles(roles);

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void shouldHandleUserWithNoRoles() {
        // given
        testUser.setRoles(new HashSet<>());

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.getAuthorities()).isEmpty();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldPreservePasswordHash() {
        // given
        String encodedPassword = "$2a$10$someComplexEncodedPasswordHash";
        testUser.setPassword(encodedPassword);
        testUser.setRoles(Set.of(userRole));

        when(userRepository.findByUsername("testuser"))
                .thenReturn(Optional.of(testUser));

        // when
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // then
        assertThat(userDetails.getPassword()).isEqualTo(encodedPassword);
    }
}
