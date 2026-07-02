package com.swp.parking.controller;

import com.swp.parking.dto.request.CreateSubscriptionRequest;
import com.swp.parking.dto.request.SubscriptionRegisterRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.CreateSubscriptionResponse;
import com.swp.parking.dto.response.FeePackageResponse;
import com.swp.parking.dto.response.MyVehicleResponse;
import com.swp.parking.dto.response.RegisterSubscriptionResponse;
import com.swp.parking.service.FeePackageService;
import com.swp.parking.service.FeeSubscriptionService;
import com.swp.parking.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class FeeSubscriptionController {

    private final FeePackageService feePackageService;
    private final FeeSubscriptionService feeSubscriptionService;
    private final SubscriptionService subscriptionService;

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/api/v1/fee-packages")
    public ResponseEntity<ApiResponse<List<FeePackageResponse>>> getFeePackages(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long vehicleTypeId) {
        Long typeId = vehicleTypeId != null ? vehicleTypeId : category;
        List<FeePackageResponse> result = feePackageService.getFeePackages(typeId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/api/v1/fee-subscriptions/my-vehicles")
    public ResponseEntity<ApiResponse<List<MyVehicleResponse>>> getMyVehicles(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long vehicleTypeId) {
        Long typeId = vehicleTypeId != null ? vehicleTypeId : category;
        List<MyVehicleResponse> result = feeSubscriptionService.getMyVehicles(getCurrentUserId(), typeId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/api/v1/fee-subscriptions/users/{userId}/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<MyVehicleResponse>>> getVehiclesForUser(
            @PathVariable Long userId,
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long vehicleTypeId) {
        Long typeId = vehicleTypeId != null ? vehicleTypeId : category;
        List<MyVehicleResponse> result = subscriptionService.getMyVehicles(userId, typeId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/api/v1/fee-subscriptions/users/{userId}/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<RegisterSubscriptionResponse>> registerSubscriptionForUser(
            @PathVariable Long userId,
            @Valid @RequestBody SubscriptionRegisterRequest request,
            HttpServletRequest httpRequest) {
        RegisterSubscriptionResponse result = subscriptionService.registerSubscription(
                userId,
                request,
                getClientIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Tạo đăng ký gói cho user thành công"));
    }

    @PostMapping("/api/v1/fee-subscriptions")
    public ResponseEntity<ApiResponse<CreateSubscriptionResponse>> createSubscription(
            @RequestBody CreateSubscriptionRequest request) {
        CreateSubscriptionResponse result = feeSubscriptionService.createSubscription(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/api/v1/fee-subscriptions/my")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMySubscriptions() {
        return ResponseEntity.ok(ApiResponse.success(feeSubscriptionService.getMySubscriptions(getCurrentUserId())));
    }

    @PostMapping("/api/v1/fee-subscriptions/{id}/payment")
    public ResponseEntity<ApiResponse<Map<String, Object>>> paySubscription(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(feeSubscriptionService.paySubscription(getCurrentUserId(), id)));
    }

    @PatchMapping("/api/v1/fee-subscriptions/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelSubscription(@PathVariable Long id) {
        feeSubscriptionService.cancelSubscription(getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy gói thành công"));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }
        return request.getRemoteAddr();
    }
}
