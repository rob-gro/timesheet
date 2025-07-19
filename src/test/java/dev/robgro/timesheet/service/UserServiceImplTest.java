package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.ResourceAlreadyExistsException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.role.RoleService;
import dev.robgro.timesheet.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static dev.robgro.timesheet.role.RoleName.ROLE_USER;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private RoleService roleService;

    @Mock
    private UserDtoMapper userDtoMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;
    private UserDto testUserDto;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = new Role();
        testRole.setId(1L);
        testRole.setName(ROLE_USER);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setPassword("encodedPassword");
        testUser.setEmail("test@example.com");
        testUser.setActive(true);
        testUser.setRoles(Set.of(testRole));

        testUserDto = new UserDto(1L, "testUser", "password", "test@example.com", true, Set.of("ROLE_USER"));
    }

    @Test
    void getAllUsers_ShouldReturnAllUsers() {
        // Given
        List<User> users = List.of(testUser);
        when(userRepository.findAll()).thenReturn(users);
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        List<UserDto> result = userService.getAllUsers();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUserDto);
        verify(userRepository).findAll();
        verify(userDtoMapper).apply(testUser);
    }

    @Test
    void getAllUsers_WhenNoUsers_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<UserDto> result = userService.getAllUsers();

        // Then
        assertThat(result).isEmpty();
        verify(userRepository).findAll();
        verifyNoInteractions(userDtoMapper);
    }

    @Test
    void getUserById_WhenUserExists_ShouldReturnUser() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        UserDto result = userService.getUserById(1L);

        // Then
        assertThat(result).isEqualTo(testUserDto);
        verify(userRepository).findById(1L);
        verify(userDtoMapper).apply(testUser);
    }

    @Test
    void getUserById_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.getUserById(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User ")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verifyNoInteractions(userDtoMapper);
    }

    @Test
    void getUsersByRole_ShouldReturnUsersWithSpecificRole() {
        // Given
        String roleName = "ROLE_USER";
        List<User> users = List.of(testUser);
        when(userRepository.findByRoleName(roleName)).thenReturn(users);
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        List<UserDto> result = userService.getUsersByRole(roleName);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testUserDto);
        verify(userRepository).findByRoleName(roleName);
        verify(userDtoMapper).apply(testUser);
    }

    @Test
    void searchUsers_ShouldReturnPagedResults() {
        // Given
        Boolean active = true;
        String username = "test";
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(testUser));
        when(userRepository.findByActiveAndUsername(active, username, pageable)).thenReturn(userPage);
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        Page<UserDto> result = userService.searchUsers(active, username, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findByActiveAndUsername(active, username, pageable);
    }

    @Test
    void createUser_WithValidData_ShouldCreateUser() {
        // Given
        UserDto newUserDto = new UserDto(null, "newUser", "password", "new@example.com", true, null);
        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newUser");
        newUser.setPassword("password");
        newUser.setEmail("new@example.com");
        newUser.setActive(true);

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userDtoMapper.apply(newUser)).thenReturn(newUserDto);

        // When
        UserDto result = userService.createUser(newUserDto);

        // Then
        assertThat(result).isEqualTo(newUserDto);
        verify(userRepository).existsByUsername("newUser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(userRepository).save(any(User.class));
        verify(userDtoMapper).apply(newUser);
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowResourceAlreadyExistsException() {
        // Given
        UserDto newUserDto = new UserDto(null, "existingUser", "password", "new@example.com", true, null);
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUserDto))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("username")
                .hasMessageContaining("existingUser");

        verify(userRepository).existsByUsername("existingUser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithExistingEmail_ShouldThrowResourceAlreadyExistsException() {
        // Given
        UserDto newUserDto = new UserDto(null, "newUser", "password", "existing@example.com", true, null);
        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUser(newUserDto))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("email")
                .hasMessageContaining("existing@example.com");

        verify(userRepository).existsByUsername("newUser");
        verify(userRepository).existsByEmail("existing@example.com");
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_WithNullEmail_ShouldCreateUser() {
        // Given
        UserDto newUserDto = new UserDto(null, "newUser", "password", null, true, null);
        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newUser");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userDtoMapper.apply(newUser)).thenReturn(newUserDto);

        // When
        UserDto result = userService.createUser(newUserDto);

        // Then
        assertThat(result).isEqualTo(newUserDto);
        verify(userRepository).existsByUsername("newUser");
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateUser_WithValidData_ShouldUpdateUser() {
        // Given
        UserDto updateDto = new UserDto(1L, "updatedUser", null, "updated@example.com", false, Set.of("ROLE_ADMIN"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updatedUser")).thenReturn(false);
        when(userRepository.existsByEmail("updated@example.com")).thenReturn(false);
        when(roleService.getRolesByNames(Set.of("ROLE_ADMIN"))).thenReturn(Set.of(testRole));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userDtoMapper.apply(testUser)).thenReturn(updateDto);

        // When
        UserDto result = userService.updateUser(1L, updateDto);

        // Then
        assertThat(result).isEqualTo(updateDto);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(roleService).getRolesByNames(Set.of("ROLE_ADMIN"));
        verify(userDtoMapper).apply(testUser);
    }

    @Test
    void updateUser_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        UserDto updateDto = new UserDto(1L, "updatedUser", null, "updated@example.com", false, null);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WithExistingUsername_ShouldThrowResourceAlreadyExistsException() {
        // Given
        UserDto updateDto = new UserDto(1L, "existingUser", null, "updated@example.com", false, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.updateUser(1L, updateDto))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("username")
                .hasMessageContaining("existingUser");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByUsername("existingUser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WithCorrectCurrentPassword_ShouldChangePassword() {
        // Given
        String currentPassword = "currentPassword";
        String newPassword = "newPassword";
        String encodedNewPassword = "encodedNewPassword";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);

        // When
        userService.changePassword(1L, currentPassword, newPassword);

        // Then
        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches(currentPassword, "encodedPassword");
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo(encodedNewPassword);
    }

    @Test
    void changePassword_WithIncorrectCurrentPassword_ShouldThrowValidationException() {
        // Given
        String currentPassword = "wrongPassword";
        String newPassword = "newPassword";

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(currentPassword, testUser.getPassword())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(1L, currentPassword, newPassword))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Current password is incorrect");

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches(currentPassword, testUser.getPassword());
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.changePassword(1L, "current", "new"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void resetPassword_WhenUserExists_ShouldResetPasswordAndReturnTempPassword() {
        // Given
        String encodedTempPassword = "encodedTempPassword";
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(anyString())).thenReturn(encodedTempPassword);

        // When
        String tempPassword = userService.resetPassword(1L);

        // Then
        assertThat(tempPassword)
                .isNotNull()
                .hasSize(10);
        verify(userRepository).findById(1L);
        verify(passwordEncoder).encode(tempPassword);
        verify(userRepository).save(testUser);
        assertThat(testUser.getPassword()).isEqualTo(encodedTempPassword);
    }

    @Test
    void resetPassword_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.resetPassword(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User ")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void updateUserRoles_ShouldUpdateRolesSuccessfully() {
        // Given
        Set<String> roleNames = Set.of("ROLE_ADMIN", "ROLE_USER");
        Set<Role> roles = Set.of(testRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(roleService.getRolesByNames(roleNames)).thenReturn(roles);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        UserDto result = userService.updateUserRoles(1L, roleNames);

        // Then
        assertThat(result).isEqualTo(testUserDto);
        verify(userRepository).findById(1L);
        verify(roleService).getRolesByNames(roleNames);
        verify(userRepository).save(testUser);
        verify(userDtoMapper).apply(testUser);
        assertThat(testUser.getRoles()).isEqualTo(roles);
    }

    @Test
    void updateUserRoles_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        Set<String> roleNames = Set.of("ROLE_ADMIN");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.updateUserRoles(1L, roleNames))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User ")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verifyNoInteractions(roleService);
    }

    @Test
    void deleteUser_WhenUserExists_ShouldDeleteUser() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(true);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        when(userRepository.existsById(1L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User ")
                .hasMessageContaining("1");

        verify(userRepository).existsById(1L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void setUserActiveStatus_ShouldUpdateActiveStatus() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userDtoMapper.apply(testUser)).thenReturn(testUserDto);

        // When
        UserDto result = userService.setUserActiveStatus(1L, false);

        // Then
        assertThat(result).isEqualTo(testUserDto);
        verify(userRepository).findById(1L);
        verify(userRepository).save(testUser);
        verify(userDtoMapper).apply(testUser);
        assertThat(testUser.isActive()).isFalse();
    }

    @Test
    void setUserActiveStatus_WhenUserNotExists_ShouldThrowEntityNotFoundException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userService.setUserActiveStatus(1L, false))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User ")
                .hasMessageContaining("1");

        verify(userRepository).findById(1L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWithPassword_WithValidData_ShouldCreateUserWithEncodedPassword() {
        // Given
        UserDto newUserDto = new UserDto(null, "newUser", null, "new@example.com", true, Set.of("ROLE_USER"));
        String rawPassword = "rawPassword";
        String encodedPassword = "encodedPassword";
        User newUser = new User();
        newUser.setId(2L);
        newUser.setUsername("newUser");

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(roleService.getRolesByNames(Set.of("ROLE_USER"))).thenReturn(Set.of(testRole));
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userDtoMapper.apply(newUser)).thenReturn(newUserDto);

        // When
        UserDto result = userService.createUserWithPassword(newUserDto, rawPassword);

        // Then
        assertThat(result).isEqualTo(newUserDto);
        verify(userRepository).existsByUsername("newUser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode(rawPassword);
        verify(roleService).getRolesByNames(Set.of("ROLE_USER"));
        verify(userRepository).save(any(User.class));
        verify(userDtoMapper).apply(newUser);
    }

    @Test
    void createUserWithPassword_WithExistingUsername_ShouldThrowResourceAlreadyExistsException() {
        // Given
        UserDto newUserDto = new UserDto(null, "existingUser", null, "new@example.com", true, null);
        when(userRepository.existsByUsername("existingUser")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userService.createUserWithPassword(newUserDto, "password"))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("User")
                .hasMessageContaining("username")
                .hasMessageContaining("existingUser");

        verify(userRepository).existsByUsername("existingUser");
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUserWithPassword_WithNullRoles_ShouldCreateUserWithoutRoles() {
        // Given
        UserDto newUserDto = new UserDto(null, "newUser", null, "new@example.com", true, null);
        String rawPassword = "rawPassword";
        String encodedPassword = "encodedPassword";
        User newUser = new User();

        when(userRepository.existsByUsername("newUser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(newUser);
        when(userDtoMapper.apply(newUser)).thenReturn(newUserDto);

        // When
        UserDto result = userService.createUserWithPassword(newUserDto, rawPassword);

        // Then
        assertThat(result).isEqualTo(newUserDto);
        verify(userRepository).existsByUsername("newUser");
        verify(passwordEncoder).encode(rawPassword);
        verifyNoInteractions(roleService);
        verify(userRepository).save(any(User.class));
    }
}
