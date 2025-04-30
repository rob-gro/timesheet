package dev.robgro.timesheet.role;

import java.util.Set;

public record RoleUpdateDto(
        Set<String> roleNames
) {
}
