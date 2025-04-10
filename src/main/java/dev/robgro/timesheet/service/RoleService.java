package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.entity.Role;

import java.util.List;
import java.util.Set;

public interface RoleService {
    Set<Role> getRolesByNames(Set<String> roleNames);

    Role getRoleByName(String roleName);

    List<Role> getAllRoles();

}
