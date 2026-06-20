package com.swp.parking.controller;

import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.MomoOrderStatusResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.MomoOrder;
import com.swp.parking.model.enums.MomoOrderStatus;
import com.swp.parking.service.MomoQrService;
import com.swp.parking.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API quản lý đơn hàng thanh toán MoMo cá nhân.
 *
 * <pre>
 * GET  /api/payments/momo-orders/{orderId}/status  – FE polling trạng thái (user xem QR)
 * GET  /api/payments/momo-orders/my               – Lịch sử đơn hàng của user
 * POST /api/payments/momo-orders/{orderId}/admin-confirm – Admin xác nhận đã nhận tiền → kích hoạt thẻ tháng
 * GET  /api/payments/momo-orders/pending          – Admin xem danh sách đơn chờ xác nhận
 * POST /api/payments/momo-orders/{orderId}/cancel  – Hủy đơn hàng PENDING
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/momo-orders")
@RequiredArgsConstructor
public class MomoOrderController {

    private final MomoQrService momoQrService;
    private final SubscriptionService subscriptionService;

    // ─────────────────────────────────────────────────────────────────
    // FE polling: gọi mỗi 5s trong khi hiển thị màn hình QR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Lấy trạng thái đơn hàng.
     * Nếu đơn đã quá hạn mà vẫn PENDING → tự động chuyển sang CANCELLED.
     * FE dùng trường {@code paymentStatus} để điều hướng:
     *   - PENDING  → tiếp tục hiển thị QR
     *   - PAID     → hiển thị màn hình thành công
     *   - CANCELLED → thông báo hết hạn / hủy
     */
    @GetMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<MomoOrderStatusResponse>> getStatus(
            @PathVariable String orderId) {

        MomoOrder order = momoQrService.getOrder(orderId);

        // Tự động expire đơn quá hạn
        if (MomoOrderStatus.PENDING.equals(order.getPaymentStatus())
                && momoQrService.isExpired(order)) {
            order = momoQrService.cancelOrder(orderId);
            log.info("MomoOrder {} tự động CANCELLED do hết hạn", orderId);
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    // ─────────────────────────────────────────────────────────────────
    // User: xem lịch sử đơn hàng của chính mình
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<MomoOrderStatusResponse>>> getMyOrders() {
        Long userId = getCurrentUserId();
        List<MomoOrderStatusResponse> list = momoQrService.getPendingOrders().stream()
                .filter(o -> userId.equals(o.getUserId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ─────────────────────────────────────────────────────────────────
    // Admin: danh sách đơn PENDING chờ xác nhận
    // ─────────────────────────────────────────────────────────────────

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<MomoOrderStatusResponse>>> getPendingOrders() {
        List<MomoOrderStatusResponse> list = momoQrService.getPendingOrders().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ─────────────────────────────────────────────────────────────────
    // Admin: xác nhận đã nhận tiền → kích hoạt thẻ tháng
    // ─────────────────────────────────────────────────────────────────

    /**
     * Admin xác nhận thanh toán sau khi kiểm tra lịch sử chuyển khoản trên app MoMo.
     *
     * @param orderId       ID đơn hàng (trong description khi user chuyển tiền)
     * @param body          JSON tùy chọn: { "transactionId": "...", "notes": "..." }
     */
    @PostMapping("/{orderId}/admin-confirm")
    public ResponseEntity<ApiResponse<MomoOrderStatusResponse>> adminConfirm(
            @PathVariable String orderId,
            @RequestBody(required = false) Map<String, String> body) {

        String transactionId = body != null ? body.get("transactionId") : null;
        String notes         = body != null ? body.getOrDefault("notes", "Admin đã xác nhận") : "Admin đã xác nhận";

        // 1. Đánh dấu đơn hàng là PAID
        MomoOrder order = momoQrService.confirmPaid(orderId, transactionId, notes);

        // 2. Kích hoạt subscription liên kết
        try {
            subscriptionService.activateSubscription(order.getSubscriptionId(), transactionId);
        } catch (AppException e) {
            log.warn("activateSubscription failed sau khi confirmPaid – orderId={}: {}", orderId, e.getMessage());
            throw e;
        }

        log.info("Admin confirmed orderId={} → subscription {} ACTIVE", orderId, order.getSubscriptionId());

        return ResponseEntity.ok(ApiResponse.success(
                toResponse(order),
                "Xác nhận thanh toán thành công – thẻ tháng đã được kích hoạt"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Hủy đơn hàng (user hoặc admin)
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<MomoOrderStatusResponse>> cancelOrder(
            @PathVariable String orderId) {

        MomoOrder order = momoQrService.cancelOrder(orderId);

        // Nếu subscription liên kết vẫn PENDING_PAYMENT → hủy luôn
        if (order.getSubscriptionId() != null) {
            subscriptionService.cancelPendingSubscription(order.getSubscriptionId());
        }

        return ResponseEntity.ok(ApiResponse.success(
                toResponse(order), "Đã hủy đơn hàng"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private MomoOrderStatusResponse toResponse(MomoOrder order) {
        return MomoOrderStatusResponse.builder()
                .orderId(order.getOrderId())
                .paymentStatus(order.getPaymentStatus().name())
                .momoAccount(order.getMomoAccount())
                .momoName(order.getMomoName())
                .amount(order.getAmount())
                .description(order.getDescription())
                .qrCodeData(order.getQrCodeData())
                .expiredAt(order.getExpiredAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .expired(momoQrService.isExpired(order))
                .notes(order.getNotes())
                .build();
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
