package com.swp.parking.controller;

import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.VNPayOrderStatusResponse;
import com.swp.parking.model.VNPayOrder;
import com.swp.parking.model.enums.VNPayOrderStatus;
import com.swp.parking.service.SubscriptionService;
import com.swp.parking.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Endpoints tích hợp VNPay.
 *
 * <pre>
 * GET  /api/payments/vnpay/orders/{txnRef}/status  – FE polling trạng thái
 * GET  /api/payments/vnpay/ipn                     – VNPay gọi server-to-server sau khi TT xong
 * GET  /api/payments/vnpay/return                  – VNPay redirect browser user sau khi TT
 * POST /api/payments/vnpay/orders/{txnRef}/cancel  – Hủy đơn PENDING
 * </pre>
 *
 * Lưu ý: Endpoint tạo link thanh toán nằm ở SubscriptionController (/api/subscriptions/register),
 * vì việc tạo đơn gắn liền với việc tạo subscription + invoice.
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;
    private final SubscriptionService subscriptionService;

    // URL trang kết quả trên FE (dùng để redirect sau khi xử lý return)
    @Value("${vnpay.fe-result-url}")
    private String feResultUrl;

    // ─────────────────────────────────────────────────────────────────
    // Polling trạng thái đơn hàng (FE gọi sau khi user thanh toán)
    // ─────────────────────────────────────────────────────────────────

    /**
     * FE polling mỗi vài giây để biết trạng thái thanh toán.
     * Tự động cancel đơn hết hạn.
     */
    @GetMapping("/orders/{txnRef}/status")
    public ResponseEntity<ApiResponse<VNPayOrderStatusResponse>> getStatus(
            @PathVariable String txnRef) {

        VNPayOrder order = vnPayService.getOrder(txnRef);

        // Tự động expire đơn quá hạn còn PENDING
        if (VNPayOrderStatus.PENDING.equals(order.getStatus())
                && vnPayService.isExpired(order)) {
            order = vnPayService.cancelOrder(txnRef);
            log.info("[VNPay] Đơn {} tự động CANCELLED do hết hạn", txnRef);
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(order)));
    }

    // ─────────────────────────────────────────────────────────────────
    // IPN — VNPay gọi server-to-server sau khi giao dịch hoàn tất
    // ─────────────────────────────────────────────────────────────────

    /**
     * Nhận IPN từ VNPay. Phải trả về JSON chuẩn trong vòng 5 giây.
     * VNPay sẽ gọi lại tối đa 3 lần nếu không nhận được phản hồi hợp lệ.
     */
    @GetMapping("/ipn")
    public ResponseEntity<Map<String, String>> handleIpn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        String txnRef = params.get("vnp_TxnRef");

        log.info("[VNPay IPN] Nhận IPN – txnRef={}, responseCode={}",
                txnRef, params.get("vnp_ResponseCode"));

        boolean processed = vnPayService.processIpn(params);

        if (!processed) {
            // Chữ ký sai hoặc không tìm thấy đơn
            return ResponseEntity.ok(Map.of(
                    "RspCode", "97",
                    "Message", "Invalid Signature or Order not found"));
        }

        // Kích hoạt subscription nếu thanh toán thành công
        VNPayOrder order = vnPayService.getOrder(txnRef);
        if (VNPayOrderStatus.PAID.equals(order.getStatus())
                && order.getSubscriptionId() != null) {
            try {
                subscriptionService.activateSubscriptionVnpay(
                        order.getSubscriptionId(),
                        order.getVnpTransactionNo());
                log.info("[VNPay IPN] Subscription {} đã ACTIVE", order.getSubscriptionId());
            } catch (Exception e) {
                log.error("[VNPay IPN] Lỗi kích hoạt subscription {}: {}",
                        order.getSubscriptionId(), e.getMessage());
                // Vẫn trả OK để VNPay không gọi lại — lỗi này do internal, sẽ xử lý thủ công
            }
        }

        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Return URL — VNPay redirect browser user sau khi thanh toán
    // ─────────────────────────────────────────────────────────────────

    /**
     * VNPay redirect browser user về đây sau khi hoàn tất thanh toán.
     * BE xác thực chữ ký rồi chuyển hướng FE kèm kết quả.
     */
    @GetMapping("/return")
    public void handleReturn(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Map<String, String> params = extractParams(request);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");

        log.info("[VNPay Return] txnRef={}, responseCode={}", txnRef, responseCode);

        boolean success = vnPayService.verifyReturn(params);

        // Lấy subscriptionId từ đơn hàng (nếu có) để FE có thể cập nhật UI
        String subscriptionId = "";
        try {
            VNPayOrder order = vnPayService.getOrder(txnRef);
            if (order.getSubscriptionId() != null) {
                subscriptionId = String.valueOf(order.getSubscriptionId());
            }
        } catch (Exception ignored) { }

        // Chuyển hướng FE kèm kết quả
        String redirectUrl = feResultUrl
                + "?status=" + (success ? "success" : "failed")
                + "&txnRef=" + txnRef
                + "&responseCode=" + responseCode
                + (subscriptionId.isEmpty() ? "" : "&subscriptionId=" + subscriptionId);

        response.sendRedirect(redirectUrl);
    }

    // ─────────────────────────────────────────────────────────────────
    // Hủy đơn hàng
    // ─────────────────────────────────────────────────────────────────

    @PostMapping("/orders/{txnRef}/cancel")
    public ResponseEntity<ApiResponse<VNPayOrderStatusResponse>> cancelOrder(
            @PathVariable String txnRef) {

        VNPayOrder order = vnPayService.cancelOrder(txnRef);

        if (order.getSubscriptionId() != null) {
            subscriptionService.cancelPendingSubscription(order.getSubscriptionId());
        }

        return ResponseEntity.ok(ApiResponse.success(toResponse(order), "Đã hủy đơn hàng VNPay"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    /** Lấy tất cả query params từ HttpServletRequest vào Map. */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                params.put(key, values[0]);
            }
        });
        return params;
    }

    private VNPayOrderStatusResponse toResponse(VNPayOrder order) {
        return VNPayOrderStatusResponse.builder()
                .txnRef(order.getTxnRef())
                .status(order.getStatus().name())
                .amount(order.getAmount())
                .orderInfo(order.getOrderInfo())
                .paymentUrl(order.getPaymentUrl())
                .vnpTransactionNo(order.getVnpTransactionNo())
                .vnpResponseCode(order.getVnpResponseCode())
                .vnpBankCode(order.getVnpBankCode())
                .createdAt(order.getCreatedAt())
                .expiredAt(order.getExpiredAt())
                .paidAt(order.getPaidAt())
                .expired(vnPayService.isExpired(order))
                .build();
    }
}
