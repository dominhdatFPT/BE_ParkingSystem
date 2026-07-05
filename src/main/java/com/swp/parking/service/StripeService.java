package com.swp.parking.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.StripeOrder;
import com.swp.parking.model.enums.StripeOrderStatus;
import com.swp.parking.repository.StripeOrderRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Xử lý toàn bộ nghiệp vụ Stripe:
 *   1. Tạo PaymentIntent (thay thế việc tạo link VNPay)
 *   2. Xác minh chữ ký webhook và cập nhật trạng thái đơn
 *   3. Truy vấn trạng thái StripeOrder để FE polling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${stripe.currency:vnd}")
    private String defaultCurrency;

    /** Đơn hết hạn sau N phút (tương tự VNPay) */
    @Value("${stripe.order-expire-minutes:30}")
    private int orderExpireMinutes;

    private final StripeOrderRepository stripeOrderRepository;

    /** Khởi tạo Stripe SDK với secret key khi bean được tạo */
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe SDK khởi tạo thành công (currency mặc định: {})", defaultCurrency);
    }

    // ─────────────────────────────────────────────────────────────────
    // 1. Tạo PaymentIntent
    // ─────────────────────────────────────────────────────────────────

    /**
     * Tạo Stripe PaymentIntent và lưu StripeOrder vào DB.
     *
     * <p>Với VND (zero-decimal): truyền {@code amount} nguyên vẹn (ví dụ 300000 = 300.000 đ).
     * Với USD/EUR: nhân {@code amount} * 100 trước khi gọi (FE thường dùng USD cho sandbox).
     *
     * @param userId         ID user đăng ký
     * @param subscriptionId ID FeeSubscription vừa tạo
     * @param invoiceId      ID FeeSubscriptionInvoice kỳ đầu
     * @param amount         Số tiền (VND — không nhân 100)
     * @param description    Mô tả giao dịch (tên gói)
     * @return StripeOrder đã lưu, chứa paymentIntentId và clientSecret
     */
    @Transactional
    public StripeOrder createPaymentIntent(Long userId, Long subscriptionId,
                                           Long invoiceId, Long amount, String description) {
        try {
            // VND là zero-decimal currency → truyền amount nguyên vẹn
            // Nếu đổi sang USD: amount * 100
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amount)
                    .setCurrency(defaultCurrency)
                    .setDescription(description)
                    .putMetadata("subscriptionId", String.valueOf(subscriptionId))
                    .putMetadata("invoiceId", String.valueOf(invoiceId))
                    .putMetadata("userId", String.valueOf(userId))
                    // Cho phép thanh toán thẻ
                    .addPaymentMethodType("card")
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            StripeOrder order = StripeOrder.builder()
                    .paymentIntentId(paymentIntent.getId())
                    .userId(userId)
                    .subscriptionId(subscriptionId)
                    .invoiceId(invoiceId)
                    .amount(amount)
                    .currency(defaultCurrency)
                    .description(description)
                    .clientSecret(paymentIntent.getClientSecret())
                    .status(StripeOrderStatus.PENDING)
                    .expiredAt(LocalDateTime.now().plusMinutes(orderExpireMinutes))
                    .build();

            StripeOrder saved = stripeOrderRepository.save(order);
            log.info("Tạo Stripe PaymentIntent {} cho subscription {}, amount={} {}",
                    paymentIntent.getId(), subscriptionId, amount, defaultCurrency);
            return saved;

        } catch (StripeException e) {
            log.error("Lỗi tạo Stripe PaymentIntent: {}", e.getMessage());
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    "Không thể tạo giao dịch Stripe: " + e.getUserMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // 2. Xử lý Webhook
    // ─────────────────────────────────────────────────────────────────

    /**
     * Xác minh chữ ký webhook Stripe và cập nhật trạng thái StripeOrder.
     *
     * <p>Trả về {@code Optional<StripeOrder>} chứa đơn đã cập nhật nếu là sự kiện
     * {@code payment_intent.succeeded} — để Controller gọi activateSubscription.
     * Trả về {@code Optional.empty()} cho các event khác (đã bỏ qua).
     *
     * @param payload   Raw request body dưới dạng String
     * @param sigHeader Giá trị header {@code Stripe-Signature}
     * @return Optional StripeOrder nếu đơn SUCCEEDED, rỗng nếu event không liên quan
     */
    @Transactional
    public Optional<StripeOrder> handleWebhook(String payload, String sigHeader) {
        Event event;
        try {
            // Xác minh HMAC-SHA256 chữ ký của Stripe
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Stripe webhook: chữ ký không hợp lệ — {}", e.getMessage());
            throw new AppException(HttpStatus.BAD_REQUEST, "Chữ ký webhook Stripe không hợp lệ");
        }

        log.info("Stripe webhook nhận event: {}", event.getType());

        return switch (event.getType()) {
            case "payment_intent.succeeded" -> handleSucceeded(event);
            case "payment_intent.payment_failed" -> {
                handleFailed(event);
                yield Optional.empty();
            }
            default -> {
                log.debug("Stripe event bỏ qua: {}", event.getType());
                yield Optional.empty();
            }
        };
    }

    /** Cập nhật StripeOrder → SUCCEEDED, trả về để Controller kích hoạt subscription */
    private Optional<StripeOrder> handleSucceeded(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) {
            log.warn("Stripe payment_intent.succeeded: không deserialize được PaymentIntent");
            return Optional.empty();
        }

        PaymentIntent pi = (PaymentIntent) deserializer.getObject().get();
        String chargeId = pi.getLatestCharge();

        return stripeOrderRepository.findByPaymentIntentId(pi.getId()).map(order -> {
            order.setStatus(StripeOrderStatus.SUCCEEDED);
            order.setStripeChargeId(chargeId);
            order.setPaidAt(LocalDateTime.now());
            StripeOrder saved = stripeOrderRepository.save(order);
            log.info("Stripe PaymentIntent {} → SUCCEEDED, charge={}", pi.getId(), chargeId);
            return saved;
        });
    }

    /** Cập nhật StripeOrder → FAILED */
    private void handleFailed(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        if (deserializer.getObject().isEmpty()) return;

        PaymentIntent pi = (PaymentIntent) deserializer.getObject().get();
        String failureMsg = pi.getLastPaymentError() != null
                ? pi.getLastPaymentError().getMessage() : "Thanh toán thất bại";

        stripeOrderRepository.findByPaymentIntentId(pi.getId()).ifPresent(order -> {
            order.setStatus(StripeOrderStatus.FAILED);
            order.setFailureMessage(failureMsg);
            stripeOrderRepository.save(order);
            log.info("Stripe PaymentIntent {} → FAILED: {}", pi.getId(), failureMsg);
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // 3. Truy vấn trạng thái
    // ─────────────────────────────────────────────────────────────────

    /**
     * Lấy StripeOrder theo paymentIntentId — FE gọi để polling kết quả.
     *
     * @throws AppException 404 nếu không tìm thấy
     */
    public StripeOrder getOrder(String paymentIntentId) {
        return stripeOrderRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Không tìm thấy Stripe order: " + paymentIntentId));
    }

    // ─────────────────────────────────────────────────────────────────
    // 4. Xác nhận thủ công (fallback khi webhook chưa đến)
    // ─────────────────────────────────────────────────────────────────

    /**
     * FE gọi ngay sau khi {@code stripe.confirmCardPayment} trả về succeeded.
     * Verify lại với Stripe API và cập nhật DB nếu thực sự thành công.
     * Idempotent — gọi nhiều lần vẫn an toàn.
     *
     * @return StripeOrder sau khi cập nhật (có thể đã SUCCEEDED từ trước)
     */
    @Transactional
    public StripeOrder confirmIfSucceeded(String paymentIntentId) {
        StripeOrder order = getOrder(paymentIntentId);

        // Đã xử lý rồi (webhook hoặc confirm trước đó), không cần làm gì
        if (order.getStatus() == StripeOrderStatus.SUCCEEDED) {
            return order;
        }

        try {
            PaymentIntent pi = PaymentIntent.retrieve(paymentIntentId);

            if ("succeeded".equals(pi.getStatus())) {
                order.setStatus(StripeOrderStatus.SUCCEEDED);
                order.setStripeChargeId(pi.getLatestCharge());
                order.setPaidAt(LocalDateTime.now());
                StripeOrder saved = stripeOrderRepository.save(order);
                log.info("confirmIfSucceeded: PaymentIntent {} → SUCCEEDED", paymentIntentId);
                return saved;
            }

            log.info("confirmIfSucceeded: PaymentIntent {} trạng thái = {}", paymentIntentId, pi.getStatus());
            return order;

        } catch (StripeException e) {
            log.error("confirmIfSucceeded lỗi retrieve {}: {}", paymentIntentId, e.getMessage());
            return order;
        }
    }
}
