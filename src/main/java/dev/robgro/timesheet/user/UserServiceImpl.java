package dev.robgro.timesheet.user;

import dev.robgro.timesheet.exception.EntityNotFoundException;
import dev.robgro.timesheet.exception.ResourceAlreadyExistsException;
import dev.robgro.timesheet.exception.ValidationException;
import dev.robgro.timesheet.role.Role;
import dev.robgro.timesheet.role.RoleService;
import dev.robgro.timesheet.seller.Seller;
import dev.robgro.timesheet.seller.SellerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserDtoMapper userDtoMapper;
    private final SellerRepository sellerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userDtoMapper)
                .orElseThrow(() -> new EntityNotFoundException("User ", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getUsersByRole(String roleName) {
        return userRepository.findByRoleName(roleName).stream()
                .map(userDtoMapper)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(Boolean active, String username, Pageable pageable) {
        return userRepository.findByActiveAndUsername(active, username, pageable)
                .map(userDtoMapper);
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userRepository.existsByUsername(userDto.username())) {
            throw new ResourceAlreadyExistsException("User", "username", userDto.username());
        }
        if (userDto.email() != null && userRepository.existsByEmail(userDto.email())) {
            throw new ResourceAlreadyExistsException("User", "email", userDto.email());
        }

        User user = new User();
        user.setUsername(userDto.username());
        user.setPassword(userDto.password());
        user.setEmail(userDto.email());
        user.setActive(userDto.active());

        User savedUser = userRepository.save(user);
        return userDtoMapper.apply(savedUser);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userDto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (!user.getUsername().equals(userDto.username()) &&
                userRepository.existsByUsername(userDto.username())) {
            throw new ResourceAlreadyExistsException("User", "username", userDto.username());
        }

        if (userDto.email() != null && !userDto.email().equals(user.getEmail()) &&
                userRepository.existsByEmail(userDto.email())) {
            throw new ResourceAlreadyExistsException("User", "email", userDto.email());
        }

        user.setUsername(userDto.username());
        user.setEmail(userDto.email());
        user.setActive(userDto.active());

        if (userDto.roles() != null && !userDto.roles().isEmpty()) {
            Set<Role> roles = roleService.getRolesByNames(userDto.roles());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        return userDtoMapper.apply(savedUser);
    }

    @Override
    @Transactional
    public void changePassword(Long id, String currentPassword, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User", id));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ValidationException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    @Transactional
    @Override
    public String resetPassword(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User ", id));

        String tempPassword = generateRandomPassword();
        user.setPassword(passwordEncoder.encode(tempPassword));

        userRepository.save(user);
        return tempPassword;
    }

    @Override
    @Transactional
    public UserDto updateUserRoles(Long id, Set<String> roleNames) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User ", id));

        Set<Role> roles = roleService.getRolesByNames(roleNames);
        user.setRoles(roles);
        User savedUser = userRepository.save(user);
        return userDtoMapper.apply(savedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User ", id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional
    public UserDto setUserActiveStatus(Long id, boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User ", id));

        user.setActive(active);
        User savedUser = userRepository.save(user);
        return userDtoMapper.apply(savedUser);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto createUserWithPassword(UserDto userDto, String rawPassword) {

        if (userRepository.existsByUsername(userDto.username())) {
            throw new ResourceAlreadyExistsException("User", "username", userDto.username());
        }
        if (userDto.email() != null && userRepository.existsByEmail(userDto.email())) {
            throw new ResourceAlreadyExistsException("User", "email", userDto.email());
        }

        User user = new User();
        user.setUsername(userDto.username());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(userDto.email());
        user.setActive(userDto.active());

        if (userDto.roles() != null && !userDto.roles().isEmpty()) {
            Set<Role> roles = roleService.getRolesByNames(userDto.roles());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        log.debug("Created new user: {}", savedUser.getUsername());

        return userDtoMapper.apply(savedUser);
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
    }

    @Override
    @Transactional
    public void setDefaultSeller(Long userId, Long sellerId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        if (sellerId != null) {
            Seller seller = sellerRepository.findById(sellerId)
                    .orElseThrow(() -> new RuntimeException("Seller not found with id: " + sellerId));
            user.setDefaultSeller(seller);
        } else {
            user.setDefaultSeller(null);
        }
        userRepository.save(user);
        log.info("Set default seller {} for user {}", sellerId, userId);
    }
}
