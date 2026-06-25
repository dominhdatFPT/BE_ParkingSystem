package com.swp.parking.controller;

import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.VNPayOrderStatusResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.VNPayOrder;
import com.swp.parking.model.enums.VNPayOrderStatus;
import com.swp.parking.repository.VNPayOrderRepository;
import com.swp.parking.service.SubscriptionService;
import com.swp.parking.service.VNPayService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller mock cho VNPay — CHỈ ACTIVE khi profile != "prod".
 *
 * Mục đích: test toàn bộ flow IPN/return mà không cần tài khoản VNPay sandbox thật.
 * Chữ ký HMAC-SHA512 được sinh đúng chuẩn VNPay → đi qua đúng code path thật.
 *
 * <pre>
 * POST /api/dev/vnpay/simulate-payment  – Giả lập VNPay gọi IPN sau thanh toán
 * GET  /api/dev/vnpay/orders            – Xem danh sách đơn VNPay trong DB
 * GET  /api/dev/vnpay/mock-return-url   – Xem URL return sẽ redirect về FE
 * </pre>
 *
 * Cách test bằng curl / Postman:
 * <pre>
 * # Bước 1: Đăng ký gói cước → lấy vnpTxnRef từ response
 * POST /api/subscriptions/register  (cần JWT)
 *
 * # Bước 2: Giả lập VNPay xác nhận thanh toán thành công
 * POST /api/dev/vnpay/simulate-payment
 * Body: { "txnRef": "VNP1234567890", "success": true, "bankCode": "NCB" }
 *
 * # Kết quả: subscription chuyển PENDING_PAYMENT → ACTIVE
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/dev/vnpay")
@RequiredArgsConstructor
@Profile("!prod")  // Không active trên môi trường production
public class VNPayMockController {

    private final VNPayService vnPayService;
    private final VNPayOrderRepository vnPayOrderRepository;
    private final SubscriptionService subscriptionService;

    @Value("${vnpay.fe-result-url}")
    private String feResultUrl;

    // ─────────────────────────────────────────────────────────────────
    // Giả lập VNPay gọi IPN
    // ─────────────────────────────────────────────────────────────────

    /**
     * Giả lập VNPay gọi IPN callback.
     * Tự động sinh chữ ký HMAC-SHA512 hợp lệ rồi gọi đúng processIpn() → kết quả như thật.
     */
    @PostMapping("/simulate-payment")
    public ResponseEntity<ApiResponse<SimulateResult>> simulatePayment(
            @RequestBody SimulateRequest request) {

        String txnRef = request.getTxnRef();
        boolean success = request.isSuccess();
        String bankCode = request.getBankCode() != null ? request.getBankCode() : "NCB";

        log.info("[VNPay MOCK] Giả lập IPN – txnRef={}, success={}, bank={}", txnRef, success, bankCode);

        // 1. Tìm đơn hàng
        VNPayOrder order = vnPayService.getOrder(txnRef);

        if (!VNPayOrderStatus.PENDING.equals(order.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT,
                    "Đơn " + txnRef + " đang ở trạng thái " + order.getStatus() + ", không thể giả lập lại");
        }

        // 2. Sinh params IPN có chữ ký thật
        Map<String, String> mockParams = vnPayService.buildMockIpnParams(order, success, bankCode);

        // 3. Gọi đúng processIpn() như VNPay thật
        boolean processed = vnPayService.processIpn(mockParams);
        if (!processed) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "processIpn() thất bại — kiểm tra log để biết nguyên nhân");
        }

        // 4. Nếu thành công → kích hoạt subscription
        String subscriptionResult = "N/A";
        VNPayOrder updated = vnPayService.getOrder(txnRef);
        if (VNPayOrderStatus.PAID.equals(updated.getStatus()) && updated.getSubscriptionId() != null) {
            try {
                subscriptionService.activateSubscriptionVnpay(
                        updated.getSubscriptionId(),
                        updated.getVnpTransactionNo());
                subscriptionResult = "Subscription " + updated.getSubscriptionId() + " → ACTIVE";
            } catch (Exception e) {
                subscriptionResult = "Lỗi kích hoạt subscription: " + e.getMessage();
            }
        } else if (!success) {
            subscriptionResult = "Thanh toán thất bại — subscription vẫn PENDING_PAYMENT";
        }

        // 5. Sinh URL mà BE sẽ redirect về FE (để test FE)
        String mockReturnUrl = feResultUrl
                + "?status=" + (success ? "success" : "failed")
                + "&txnRef=" + txnRef
                + "&responseCode=" + (success ? "00" : "11")
                + (updated.getSubscriptionId() != null ? "&subscriptionId=" + updated.getSubscriptionId() : "");

        SimulateResult result = new SimulateResult();
        result.setTxnRef(txnRef);
        result.setOrderStatus(updated.getStatus().name());
        result.setSubscriptionResult(subscriptionResult);
        result.setMockReturnUrl(mockReturnUrl);
        result.setIpnParams(mockParams);

        log.info("[VNPay MOCK] Hoàn tất – txnRef={}, orderStatus={}", txnRef, updated.getStatus());

        return ResponseEntity.ok(ApiResponse.success(result,
                success ? "Giả lập thanh toán THÀNH CÔNG" : "Giả lập thanh toán THẤT BẠI"));
    }

    // ─────────────────────────────────────────────────────────────────
    // Xem danh sách đơn VNPay
    // ─────────────────────────────────────────────────────────────────

    /** Xem tất cả đơn VNPay trong DB (tiện tra cứu txnRef để test). */
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<VNPayOrderStatusResponse>>> listOrders(
            @RequestParam(required = false) String status) {

        List<VNPayOrder> orders;
        if (status != null) {
            try {
                VNPayOrderStatus s = VNPayOrderStatus.valueOf(status.toUpperCase());
                orders = vnPayOrderRepository.findByStatusOrderByCreatedAtDesc(s);
            } catch (IllegalArgumentException e) {
                throw new AppException(HttpStatus.BAD_REQUEST,
                        "status không hợp lệ. Dùng: PENDING | PAID | FAILED | CANCELLED");
            }
        } else {
            orders = vnPayOrderRepository.findAll();
        }

        List<VNPayOrderStatusResponse> list = orders.stream()
                .map(o -> VNPayOrderStatusResponse.builder()
                        .txnRef(o.getTxnRef())
                        .status(o.getStatus().name())
                        .amount(o.getAmount())
                        .orderInfo(o.getOrderInfo())
                        .vnpTransactionNo(o.getVnpTransactionNo())
                        .vnpResponseCode(o.getVnpResponseCode())
                        .vnpBankCode(o.getVnpBankCode())
                        .createdAt(o.getCreatedAt())
                        .expiredAt(o.getExpiredAt())
                        .paidAt(o.getPaidAt())
                        .expired(vnPayService.isExpired(o))
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(list));
    }

    // ─────────────────────────────────────────────────────────────────
    // DTOs nội bộ
    // ─────────────────────────────────────────────────────────────────

    @Data
    public static class SimulateRequest {
        /** Mã txnRef từ response của POST /api/subscriptions/register */
        private String txnRef;
        /** true = VNPay báo thành công (00), false = thất bại (11) */
        private boolean success = true;
        /** Tên ngân hàng giả (mặc định NCB) */
        private String bankCode;
    }

    @Data
    public static class SimulateResult {
        private String txnRef;
        private String orderStatus;
        private String subscriptionResult;
        /** URL FE sẽ nhận được sau khi BE xử lý return — paste vào browser để test FE */
        private String mockReturnUrl;
        /** Params IPN đã được ký — dùng để debug */
        private Map<String, String> ipnParams;
    }
}
