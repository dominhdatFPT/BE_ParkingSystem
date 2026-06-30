package com.swp.parking.controller;

import com.swp.parking.dto.request.AdminReviewRequest;
import com.swp.parking.dto.request.VehicleRegistrationRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.VehicleRegistrationResponse;
import com.swp.parking.service.VehicleRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-registrations")
@RequiredArgsConstructor
@Slf4j
public class VehicleRegistrationController {

    private final VehicleRegistrationService service;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private String getCurrentRole() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream().findFirst()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .orElse("USER");
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VehicleRegistrationResponse>> createRegistration(
            @Valid @RequestBody VehicleRegistrationRequest body) {
        VehicleRegistrationResponse result = service.createRegistration(getCurrentUserId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Đăng ký xe thành công, chờ admin duyệt"));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<VehicleRegistrationResponse>>> getMyRegistrations() {
        return ResponseEntity.ok(ApiResponse.success(service.getMyRegistrations(getCurrentUserId())));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleRegistrationResponse>> createRegistrationForUser(
            @PathVariable Long userId,
            @Valid @RequestBody VehicleRegistrationRequest body) {
        VehicleRegistrationResponse result = service.createRegistrationForUser(userId, getCurrentUserId(), body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Tao ho so dang ky xe cho user thanh cong"));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<VehicleRegistrationResponse>>> getPendingRegistrations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.getPendingRegistrations(page, size)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Page<VehicleRegistrationResponse>>> getRegistrations(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.getRegistrations(status, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleRegistrationResponse>> getById(@PathVariable Long id) {
        VehicleRegistrationResponse result = service.getById(id, getCurrentUserId(), getCurrentRole());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<VehicleRegistrationResponse>> adminReview(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewRequest body) {
        VehicleRegistrationResponse result = service.adminReview(id, getCurrentUserId(), body);
        return ResponseEntity.ok(ApiResponse.success(result, "Xử lý đăng ký thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable Long id) {
        service.softDelete(id, getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa xe thành công"));
    }
}
