package com.swp.parking.controller;

import com.swp.parking.dto.request.ForgotPasswordRequest;
import com.swp.parking.dto.request.GoogleLoginRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
import com.swp.parking.dto.request.ResetPasswordRequest;
import com.swp.parking.dto.request.VerifyOtpRequest;
import com.swp.parking.dto.response.AuthResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.auth.refresh-cookie-name}")
    private String refreshCookieName;

    @Value("${app.auth.refresh-cookie-path}")
    private String refreshCookiePath;

    @Value("${app.auth.refresh-cookie-secure}")
    private boolean refreshCookieSecure;

    @Value("${app.auth.refresh-cookie-same-site}")
    private String refreshCookieSameSite;

    @Value("${app.auth.refresh-cookie-domain:}")
    private String refreshCookieDomain;

    @Value("${app.refresh-token.expiration-ms}")
    private long refreshTokenExpirationMs;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();

        if (request.isRememberMe() && StringUtils.hasText(response.getRefreshToken())) {
            builder.header(HttpHeaders.SET_COOKIE, createRefreshCookie(response.getRefreshToken()).toString());
        } else {
            builder.header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString());
        }

        return builder.body(response);
    }

    @PostMapping({"/register", "/signup"})
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/google-login")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }

    // ── Quên mật khẩu ────────────────────────────────────────────────

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(Map.of("message", "Mã OTP đã được gửi tới email của bạn"));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        authService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(Map.of("message", "OTP hợp lệ. Vui lòng tạo mật khẩu mới"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Đặt lại mật khẩu thành công"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponse> refreshToken(
            @CookieValue(name = "${app.auth.refresh-cookie-name}", required = false) String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token cookie is missing");
        }

        AuthResponse response = authService.refreshSession(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(name = "${app.auth.refresh-cookie-name}", required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return baseRefreshCookie(refreshToken)
                .maxAge(refreshTokenExpirationMs / 1000)
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return baseRefreshCookie("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseRefreshCookie(String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath);

        if (StringUtils.hasText(refreshCookieDomain)) {
            builder.domain(refreshCookieDomain);
        }

        return builder;
    }
}
