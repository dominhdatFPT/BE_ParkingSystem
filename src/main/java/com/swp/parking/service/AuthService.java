package com.swp.parking.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.swp.parking.config.JwtConfig;
import com.swp.parking.dto.request.GoogleLoginRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
import com.swp.parking.dto.response.AuthResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email not found"));

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "This account uses Google sign-in. Please log in with Google.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Invalid password");
        }

        user.setRole(resolveRole(user.getId()));
        String token = jwtConfig.generateToken(user.getId(), user.getRole().name());

        return mapToAuthResponse(user, token);
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.CONFLICT, "Email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.USER)
                .build();

        user = userRepository.save(user);
        user.setRole(UserRole.USER);

        // TODO: Gửi push notification chào mừng qua Firebase Admin SDK
        String token = jwtConfig.generateToken(user.getId(), user.getRole().name());

        return mapToAuthResponse(user, token);
    }

    public AuthResponse googleLogin(GoogleLoginRequest request) {
        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance()
                    .verifyIdToken(request.getIdToken());
        } catch (FirebaseAuthException ex) {
            log.warn("Firebase ID token verification failed: {}", ex.getMessage());
            throw new AppException(HttpStatus.UNAUTHORIZED,
                    "Invalid or expired Google ID token");
        } catch (IllegalStateException ex) {
            log.error("Firebase is not initialized. Check firebase-service-account.json on classpath.", ex);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Firebase is not configured on the server");
        }

        String email = decodedToken.getEmail();
        if (email == null || email.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Google account does not expose an email address");
        }

        User user = findOrCreateGoogleUser(email, decodedToken.getName());
        user.setRole(resolveRole(user.getId()));

        String token = jwtConfig.generateToken(user.getId(), user.getRole().name());
        return mapToAuthResponse(user, token);
    }

    private User findOrCreateGoogleUser(String email, String fullName) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    String safeName = (fullName == null || fullName.isBlank())
                            ? email.split("@")[0]
                            : fullName;
                    String placeholderHash = passwordEncoder.encode(UUID.randomUUID().toString());
                    User newUser = User.builder()
                            .email(email)
                            .fullName(safeName)
                            .password(placeholderHash)
                            .role(UserRole.USER)
                            .build();
                    log.info("Creating new user from Google login: {}", email);
                    return userRepository.save(newUser);
                });
    }

    private AuthResponse mapToAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
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
                .orElse(UserRole.USER);
    }
}
