package com.swp.parking.controller;

import com.swp.parking.dto.request.ChangePasswordRequest;
import com.swp.parking.dto.request.VerifyPasswordRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản lý hồ sơ tài khoản của user đang đăng nhập.
 *
 * <pre>
 * POST /api/v1/profile/verify-password – xác minh mật khẩu hiện tại (bước 1 của form đổi mật khẩu)
 * POST /api/v1/profile/change-password – đổi mật khẩu mới
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @PostMapping("/verify-password")
    public ResponseEntity<ApiResponse<Void>> verifyPassword(@Valid @RequestBody VerifyPasswordRequest request) {
        profileService.verifyCurrentPassword(getCurrentUserId(), request.getPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Mật khẩu hợp lệ"));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
