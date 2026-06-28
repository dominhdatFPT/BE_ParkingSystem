package com.swp.parking.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.swp.parking.config.JwtConfig;
import com.swp.parking.dto.request.ForgotPasswordRequest;
import com.swp.parking.dto.request.GoogleLoginRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
import com.swp.parking.dto.request.ResetPasswordRequest;
import com.swp.parking.dto.request.VerifyOtpRequest;
import com.swp.parking.dto.response.AuthResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.Customer;
import com.swp.parking.model.PasswordResetToken;
import com.swp.parking.model.RefreshToken;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.PasswordResetTokenRepository;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

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

        String refreshToken = null;
        if (request.isRememberMe()) {
            RefreshToken rt = refreshTokenService.createRefreshToken(user.getId());
            refreshToken = rt.getToken();
        }

        return mapToAuthResponse(user, token, refreshToken);
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
        ensureCustomerProfile(user);
        user.setRole(UserRole.USER);

        // TODO: Gửi push notification chào mừng qua Firebase Admin SDK
        String token = jwtConfig.generateToken(user.getId(), user.getRole().name());

        return mapToAuthResponse(user, token, null);
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
        return mapToAuthResponse(user, token, null);
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
                    User savedUser = userRepository.save(newUser);
                    ensureCustomerProfile(savedUser);
                    return savedUser;
                });
    }

    private Customer ensureCustomerProfile(User user) {
        return customerRepository.findByUser_Id(user.getId())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .user(user)
                        .build()));
    }

    // ── Quên mật khẩu ────────────────────────────────────────────────

    public void requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        String otpCode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .otpCode(otpCode)
                .verified(false)
                .used(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        passwordResetTokenRepository.save(token);
        emailService.sendOtpEmail(user.getEmail(), otpCode);
        log.info("OTP quên mật khẩu đã gửi tới email={}", user.getEmail());
    }

    public void verifyPasswordResetOtp(VerifyOtpRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByUser_EmailAndOtpCodeAndExpiresAtAfterAndUsedFalse(
                        request.getEmail(), request.getOtp(), LocalDateTime.now())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "OTP không đúng hoặc đã hết hạn"));

        token.setVerified(true);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        passwordResetTokenRepository.save(token);
        log.info("Đã xác thực OTP quên mật khẩu cho email={}", request.getEmail());
    }

    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByUser_EmailAndOtpCodeAndVerifiedTrueAndUsedFalseAndExpiresAtAfter(
                        request.getEmail(), request.getOtp(), LocalDateTime.now())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST,
                        "Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);
        log.info("Đặt lại mật khẩu thành công cho email={}", user.getEmail());
    }

    private AuthResponse mapToAuthResponse(User user, String token, String refreshToken) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .refreshToken(refreshToken)
                .build();
    }

    public String generateNewAccessToken(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.findByToken(refreshTokenValue);
        refreshTokenService.verifyExpiration(refreshToken);
        User user = refreshToken.getUser();
        UserRole role = resolveRole(user.getId());
        return jwtConfig.generateToken(user.getId(), role.name());
    }

    private UserRole resolveRole(Long userId) {
        return userRepository.findActiveEmployeeRoleByUserId(userId)
                .map(String::toUpperCase)
                .map(role -> {
                    if ("ADMIN".equals(role)) {
                        return UserRole.ADMIN;
                    }
                    // Mọi role nhân viên còn lại (SECURITY, CASHIER, MANAGER, ...) đều gộp thành STAFF
                    return UserRole.STAFF;
                })
                .orElse(UserRole.USER);
    }
}
