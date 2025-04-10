package dev.robgro.timesheet.service;

import dev.robgro.timesheet.model.dto.UserDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

public interface UserService {
    List<UserDto> getAllUsers();

    UserDto getUserById(Long id);

    UserDto createUser(UserDto userDto);

    UserDto updateUser(Long id, UserDto userDto);

    boolean changePassword(Long id, String currentPassword, String newPassword);

    void deleteUser(Long id);

    UserDto setUserActiveStatus(Long id, boolean active);

    UserDto createUserWithPassword(UserDto userDto, String rawPassword);

    UserDto updateUserRoles(Long id, Set<String> roleNames);


    @Transactional
    String resetPassword(Long id);
}
