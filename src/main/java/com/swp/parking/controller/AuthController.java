package com.swp.parking.controller;

import com.swp.parking.dto.request.FirebaseLoginRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.LoginResponse;
import com.swp.parking.service.AuthService;
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

    /**
     * Đăng nhập Google: FE gửi Firebase ID Token, BE verify và trả JWT hệ thống.
     */
    @PostMapping("/google-login")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@RequestBody FirebaseLoginRequest request) {
        log.info("Yêu cầu đăng nhập Google");
        LoginResponse response = authService.loginWithGoogle(request);
        return ResponseEntity.ok(ApiResponse.success(response));
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
