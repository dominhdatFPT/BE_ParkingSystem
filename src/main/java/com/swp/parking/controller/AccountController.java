package com.swp.parking.controller;

import com.swp.parking.dto.request.ChangeRoleRequest;
import com.swp.parking.dto.request.CreateAccountRequest;
import com.swp.parking.dto.response.AccountUserResponse;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.service.AccountService;
import com.swp.parking.service.FeeSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final FeeSubscriptionService feeSubscriptionService;

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Page<AccountUserResponse>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(accountService.getUsers(status, keyword, pageable));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AccountUserResponse> createUser(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.success(accountService.createUser(request), "Tạo tài khoản thành công");
    }

    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateUserStatus(@PathVariable Long userId) {
        accountService.updateUserStatus(userId);
        return ApiResponse.success(null, "Cập nhật trạng thái thành công");
    }

    @PatchMapping("/users/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        accountService.updateUserRole(userId, request.getRole());
        return ApiResponse.success(null, "Đổi role thành công");
    }

    @GetMapping("/employees")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Page<AccountUserResponse>> getEmployees(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ApiResponse.success(accountService.getStaffs(role, status, keyword, pageable));
    }

    @PatchMapping("/subscriptions/{subscriptionId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> cancelSubscription(@PathVariable Long subscriptionId) {
        feeSubscriptionService.cancelSubscriptionAdmin(subscriptionId);
        return ApiResponse.success(null, "Hủy gói cước thành công");
    }

    @PostMapping("/subscriptions/{subscriptionId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse<Void> paySubscription(@PathVariable Long subscriptionId) {
        feeSubscriptionService.paySubscriptionAdmin(subscriptionId);
        return ApiResponse.success(null, "Thanh toán gói cước thành công");
    }
}
