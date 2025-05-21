package dev.robgro.timesheet.service;

import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.role.RoleName;
import dev.robgro.timesheet.role.RoleRepository;
import dev.robgro.timesheet.role.RoleServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void getRolesByNames_whenNullInput_thenReturnEmptySet() {
        // given
        Set<String> roleNames = null;

        // when
        Set<Role> result = roleService.getRolesByNames(roleNames);

        // then
        assertTrue(result.isEmpty());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void getRolesByNames_whenEmptyInput_thenReturnEmptySet() {
        // given
        Set<String> roleNames = Collections.emptySet();

        // when
        Set<Role> result = roleService.getRolesByNames(roleNames);

        // then
        assertTrue(result.isEmpty());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void getRolesByNames_whenValidInput_thenReturnRoles() {
        // given
        Set<String> roleNames = new HashSet<>(Arrays.asList("ROLE_ADMIN", "ROLE_USER"));

        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ROLE_ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setName(RoleName.ROLE_USER);

        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));

        // when
        Set<Role> result = roleService.getRolesByNames(roleNames);

        // then
        assertEquals(2, result.size());
        assertTrue(result.contains(adminRole));
        assertTrue(result.contains(userRole));
        verify(roleRepository).findByName(RoleName.ROLE_ADMIN);
        verify(roleRepository).findByName(RoleName.ROLE_USER);
    }

    @Test
    void getRolesByNames_whenInvalidRoleName_thenThrowValidationException() {
        // given
        Set<String> roleNames = new HashSet<>(Arrays.asList("NONEXISTENT_ROLE"));

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> roleService.getRolesByNames(roleNames));

        assertEquals("Invalid role name: NONEXISTENT_ROLE", exception.getMessage());
        // Verify that repository was not called at all
        verifyNoInteractions(roleRepository);
    }

    @Test
    void getRoleByName_whenValidRoleExists_thenReturnRole() {
        // given
        String roleName = "ROLE_ADMIN";
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ROLE_ADMIN);

        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.of(adminRole));

        // when
        Role result = roleService.getRoleByName(roleName);

        // then
        assertNotNull(result);
        assertEquals(adminRole, result);
        verify(roleRepository).findByName(RoleName.ROLE_ADMIN);
    }

    @Test
    void getRoleByName_whenRoleDoesNotExist_thenThrowEntityNotFoundException() {
        // given
        String roleName = "ROLE_ADMIN";
        when(roleRepository.findByName(RoleName.ROLE_ADMIN)).thenReturn(Optional.empty());

        // when & then
        EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
                () -> roleService.getRoleByName(roleName));

        assertEquals("Role with name ROLE_ADMIN not found", exception.getMessage());
        verify(roleRepository).findByName(RoleName.ROLE_ADMIN);
    }

    @Test
    void getRoleByName_whenInvalidRoleName_thenThrowValidationException() {
        // given
        String roleName = "NONEXISTENT_ROLE";

        // when & then
        ValidationException exception = assertThrows(ValidationException.class,
                () -> roleService.getRoleByName(roleName));

        assertEquals("Invalid role name: NONEXISTENT_ROLE", exception.getMessage());
        verifyNoInteractions(roleRepository);
    }

    @Test
    void getAllRoles_thenReturnAllRoles() {
        // given
        Role adminRole = new Role();
        adminRole.setId(1L);
        adminRole.setName(RoleName.ROLE_ADMIN);

        Role userRole = new Role();
        userRole.setId(2L);
        userRole.setName(RoleName.ROLE_USER);

        List<Role> expectedRoles = Arrays.asList(adminRole, userRole);
        when(roleRepository.findAll()).thenReturn(expectedRoles);

        // when
        List<Role> result = roleService.getAllRoles();

        // then
        assertEquals(expectedRoles, result);
        verify(roleRepository).findAll();
    }
}
