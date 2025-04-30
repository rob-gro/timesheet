package dev.robgro.timesheet.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

public record UserDto(
        Long id,

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        String password,

        @Email
        String email,

        boolean active,

        Set<String> roles
) {
    public UserDto(Long id, String username, String password, String email) {
        this(id, username, password, email, true, new HashSet<>());
    }
}
