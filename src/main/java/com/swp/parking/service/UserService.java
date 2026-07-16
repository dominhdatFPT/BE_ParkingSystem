package com.swp.parking.service;

import com.swp.parking.dto.response.UserResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Pattern GMAIL_EMAIL_PATTERN =
            Pattern.compile(com.swp.parking.validation.ValidationPatterns.GMAIL_EMAIL, Pattern.CASE_INSENSITIVE);
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile(com.swp.parking.validation.ValidationPatterns.STRONG_PASSWORD);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        user.setRole(resolveRole(user.getId()));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long id) {
        return getUserById(id);
    }

    public UserResponse createUser(User user) {
        String email = normalizeAndValidateEmail(user.getEmail());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(HttpStatus.CONFLICT, "Email already exists");
        }
        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setEmail(email);
        if (user.getFullName() != null) {
            user.setFullName(user.getFullName().trim());
        }
        return mapToResponse(userRepository.save(user));
    }

    public UserResponse updateUser(Long id, User updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        String email = normalizeAndValidateEmail(updatedUser.getEmail());
        userRepository.findByEmailIgnoreCase(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(HttpStatus.CONFLICT, "Email already exists");
                });

        user.setFullName(updatedUser.getFullName() == null ? null : updatedUser.getFullName().trim());
        user.setEmail(email);
        user.setRole(updatedUser.getRole());

        return mapToResponse(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private UserRole resolveRole(Long userId) {
        return userRepository.findActiveEmployeeRoleByUserId(userId)
                .map(String::toUpperCase)
                .map(role -> {
                    if ("SECURITY".equals(role) || "CASHIER".equals(role)) {
                        return UserRole.STAFF;
                    }
                    try {
                        return UserRole.valueOf(role);
                    } catch (IllegalArgumentException ex) {
                        return UserRole.USER;
                    }
                })
                .orElseGet(() -> userRepository.findById(userId)
                        .map(User::getRole)
                        .orElse(UserRole.USER));
    }

    private String normalizeAndValidateEmail(String email) {
        String normalized = email == null ? null : email.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || normalized.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (!GMAIL_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email must be a Gmail address");
        }
        return normalized;
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 8 characters and contain at least one letter and one number");
        }
    }
}
