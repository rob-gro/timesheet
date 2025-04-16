package dev.robgro.timesheet.model.dto;

import java.util.Set;

public record RoleUpdateDto(
        Set<String> roleNames
) {
}
