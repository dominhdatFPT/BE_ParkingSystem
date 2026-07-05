package com.swp.parking.controller;

import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.model.StripeOrder;
import com.swp.parking.service.StripeService;
import com.swp.parking.service.SubscriptionService;
import com.swp.parking.model.enums.StripeOrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Endpoint thanh toán Stripe.
 *
 * <pre>
 * GET  /api/payments/stripe/orders/{paymentIntentId}/status  – FE polling kết quả
 * POST /api/payments/stripe/webhook                          – Stripe gọi (không cần JWT)
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/payments/stripe")
@RequiredArgsConstructor
public class StripeController {

    private final StripeService stripeService;
    private final SubscriptionService subscriptionService;

    /**
     * FE gọi ngay sau confirmCardPayment thành công — dự phòng khi webhook chưa đến.
     * Verify lại với Stripe API, cập nhật DB và kích hoạt subscription nếu succeeded.
     * Endpoint public (không cần JWT) để FE có thể gọi ngay sau khi redirect.
     */
    @PostMapping("/orders/{paymentIntentId}/confirm")
    public ResponseEntity<ApiResponse<StripeOrder>> confirmOrder(
            @PathVariable String paymentIntentId) {
        try {
            StripeOrder order = stripeService.confirmIfSucceeded(paymentIntentId);

            if (StripeOrderStatus.SUCCEEDED.equals(order.getStatus())
                    && order.getSubscriptionId() != null) {
                subscriptionService.activateSubscriptionStripe(
                        order.getSubscriptionId(), order.getStripeChargeId());
                log.info("confirmOrder: kích hoạt subscription {} qua PaymentIntent {}",
                        order.getSubscriptionId(), paymentIntentId);
            }

            order.setClientSecret(null);
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            log.error("Lỗi confirm Stripe order {}: {}", paymentIntentId, e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(null));
        }
    }

    /**
     * FE polling để biết trạng thái thanh toán sau khi confirmCardPayment.
     * Endpoint public — FE có thể gọi ngay sau khi redirect dù session hết hạn.
     */
    @GetMapping("/orders/{paymentIntentId}/status")
    public ResponseEntity<ApiResponse<StripeOrder>> getOrderStatus(
            @PathVariable String paymentIntentId) {
        StripeOrder order = stripeService.getOrder(paymentIntentId);
        // Ẩn clientSecret khỏi response status (chỉ cần khi tạo đơn)
        order.setClientSecret(null);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    /**
     * Stripe gọi webhook khi có sự kiện thanh toán.
     *
     * <p>Luồng:
     * <ol>
     *   <li>Stripe gửi POST với raw JSON + header {@code Stripe-Signature}</li>
     *   <li>StripeService xác minh HMAC-SHA256 chữ ký</li>
     *   <li>Nếu {@code payment_intent.succeeded}: kích hoạt subscription</li>
     *   <li>Trả {@code 200 OK} trong vòng 5s để Stripe không retry</li>
     * </ol>
     *
     * <p>QUAN TRỌNG: Spring Boot đọc body trước khi vào đây nếu có filter.
     * Endpoint này cần {@code consumes = "application/json"} và không được
     * đặt sau bất kỳ body-consuming filter nào.
     */
    @PostMapping(value = "/webhook", consumes = "application/json")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            Optional<StripeOrder> succeededOrder = stripeService.handleWebhook(payload, sigHeader);

            // Nếu payment_intent.succeeded → kích hoạt subscription
            succeededOrder.ifPresent(order -> {
                if (StripeOrderStatus.SUCCEEDED.equals(order.getStatus())
                        && order.getSubscriptionId() != null) {
                    subscriptionService.activateSubscriptionStripe(
                            order.getSubscriptionId(),
                            order.getStripeChargeId());
                    log.info("Stripe webhook: đã kích hoạt subscription {} qua PaymentIntent {}",
                            order.getSubscriptionId(), order.getPaymentIntentId());
                }
            });

        } catch (Exception e) {
            // Trả 400 để Stripe biết webhook thất bại (sẽ retry)
            log.error("Lỗi xử lý Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // Trả 200 trong vòng 5 giây — Stripe yêu cầu
        return ResponseEntity.ok("OK");
    }
}
