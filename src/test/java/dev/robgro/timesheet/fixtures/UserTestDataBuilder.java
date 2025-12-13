package dev.robgro.timesheet.fixtures;

import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.role.RoleName;
import dev.robgro.timesheet.user.User;
import dev.robgro.timesheet.user.UserDto;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class UserTestDataBuilder {
    private Long id = 1L;
    private String username = "testuser";
    private String password = "encodedPassword123";
    private String email = "testuser@example.com";
    private boolean active = true;
    private Set<Role> roles = new HashSet<>();

    private UserTestDataBuilder() {
    }

    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    public UserTestDataBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestDataBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserTestDataBuilder active() {
        this.active = true;
        return this;
    }

    public UserTestDataBuilder inactive() {
        this.active = false;
        return this;
    }

    public UserTestDataBuilder withRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public UserTestDataBuilder withRole(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        this.roles.add(role);
        return this;
    }

    public UserTestDataBuilder withRoles(Set<Role> roles) {
        this.roles = new HashSet<>(roles);
        return this;
    }

    public UserTestDataBuilder asAdmin() {
        Role adminRole = new Role();
        adminRole.setName(RoleName.ROLE_ADMIN);
        this.roles.add(adminRole);
        return this;
    }

    public UserTestDataBuilder asRegularUser() {
        Role userRole = new Role();
        userRole.setName(RoleName.ROLE_USER);
        this.roles.add(userRole);
        return this;
    }

    public User build() {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setActive(active);
        user.setRoles(roles);
        return user;
    }

    public UserDto buildDto() {
        Set<String> roleNames = roles.stream()
            .map(role -> role.getName().name())
            .collect(Collectors.toSet());

        return new UserDto(
            id,
            username,
            null, // password not exposed in DTO
            email,
            active,
            roleNames
        );
    }
}