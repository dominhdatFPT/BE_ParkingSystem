package com.swp.parking.controller;

import com.swp.parking.dto.request.SubscriptionRegisterRequest;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.MyVehicleResponse;
import com.swp.parking.dto.response.RegisterSubscriptionResponse;
import com.swp.parking.dto.response.SubscriptionInvoiceResponse;
import com.swp.parking.dto.response.SubscriptionResponse;
import com.swp.parking.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Quản lý toàn bộ vòng đời thẻ tháng bãi đỗ xe.
 * Tất cả endpoint yêu cầu JWT hợp lệ.
 *
 * <pre>
 * GET    /api/subscriptions/my-vehicles     – danh sách xe để chọn đăng ký
 * POST   /api/subscriptions/register        – đăng ký thẻ tháng → payUrl MoMo
 * PATCH  /api/subscriptions/{id}/cancel     – hủy thẻ tháng đang ACTIVE
 * POST   /api/subscriptions/cancel-renew    – tắt tự động gia hạn
 * GET    /api/subscriptions/my              – lịch sử thẻ tháng của user
 * </pre>
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * Lấy danh sách phương tiện của user.
     * Truyền {@code vehicleTypeId} để lọc xe theo loại phù hợp với gói đang xem.
     */
    @GetMapping("/my-vehicles")
    public ResponseEntity<ApiResponse<List<MyVehicleResponse>>> getMyVehicles(
            @RequestParam(required = false) Long vehicleTypeId) {
        List<MyVehicleResponse> data = subscriptionService.getMyVehicles(getCurrentUserId(), vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Đăng ký thẻ tháng.
     * Flow: tạo subscription PENDING_PAYMENT → gọi MoMo → trả payUrl/deeplink/qrCodeUrl.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterSubscriptionResponse>> registerSubscription(
            @Valid @RequestBody SubscriptionRegisterRequest request) {
        RegisterSubscriptionResponse data = subscriptionService.registerSubscription(getCurrentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(data,
                "Đăng ký thẻ tháng thành công, vui lòng hoàn tất thanh toán trên MoMo"));
    }

    /**
     * Hủy thẻ tháng đang ACTIVE theo ID.
     * Subscription chuyển ngay sang CANCELLED, partnerToken bị xóa để ngăn auto-renew.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelSubscription(@PathVariable Long id) {
        SubscriptionResponse data = subscriptionService.cancelSubscription(getCurrentUserId(), id);
        return ResponseEntity.ok(ApiResponse.success(data, "Hủy thẻ tháng thành công"));
    }

    /**
     * Tắt tự động gia hạn cho gói đang ACTIVE.
     * Subscription vẫn còn hiệu lực đến endDate, chỉ không gia hạn thêm sau đó.
     */
    @PostMapping("/cancel-renew")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> cancelAutoRenew() {
        SubscriptionResponse data = subscriptionService.cancelAutoRenew(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(data, "Đã hủy tự động gia hạn thành công"));
    }

    /**
     * Lịch sử tất cả thẻ tháng (mọi trạng thái) của user hiện tại.
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions() {
        List<SubscriptionResponse> data = subscriptionService.getUserSubscriptions(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * Lịch sử hoá đơn thanh toán thẻ tháng (bao gồm PENDING / SUCCESS / FAILED).
     */
    @GetMapping("/my-invoices")
    public ResponseEntity<ApiResponse<List<SubscriptionInvoiceResponse>>> getMyInvoices() {
        List<SubscriptionInvoiceResponse> data = subscriptionService.getUserInvoices(getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
