package com.swp.parking.controller;

import com.swp.parking.dto.request.ForgotPasswordRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
import com.swp.parking.dto.request.ResetPasswordRequest;
import com.swp.parking.dto.request.VerifyOtpRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.LoginResponse;
import com.swp.parking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        log.info("Yêu cầu đăng nhập, email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Yêu cầu đăng ký tài khoản, email={}", request.getEmail());
        String message = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Long>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Yêu cầu quên mật khẩu, email={}", request.getEmail());
        Long requestId = authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResponse.success(requestId));
    }

    @PostMapping("/verify-forgot-password-otp")
    public ResponseEntity<ApiResponse<String>> verifyForgotPasswordOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Xác thực OTP quên mật khẩu, requestId={}", request.getRequestId());
        String resetToken = authService.verifyPasswordResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success(resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Đặt lại mật khẩu mới bằng resetToken={}", request.getResetToken());
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @PostMapping("/test-bcrypt")
    public ResponseEntity<Boolean> testBcrypt(@RequestBody java.util.Map<String, String> body) {
        String raw = body.get("raw");
        String hash = body.get("hash");
        boolean result = passwordEncoder.matches(raw, hash);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/generate-hash")
    public ResponseEntity<String> generateHash() {
        String hash = passwordEncoder.encode("123456");
        return ResponseEntity.ok(hash);
    }
}
