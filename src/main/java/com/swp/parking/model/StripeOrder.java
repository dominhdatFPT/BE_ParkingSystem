package com.swp.parking.model;

import com.swp.parking.model.enums.StripeOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeOrder {

    /** ID PaymentIntent từ Stripe (pi_xxx) — dùng làm PK */
    @Id
    @Column(name = "payment_intent_id", nullable = false, length = 100)
    private String paymentIntentId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    /** Số tiền thực tế theo đơn vị tiền tệ (VND → không nhân 100) */
    @Column(name = "amount", nullable = false)
    private Long amount;

    /** Mã tiền tệ ISO 4217, mặc định "vnd" */
    @Column(name = "currency", length = 10)
    @Builder.Default
    private String currency = "vnd";

    @Column(name = "description", length = 500)
    private String description;

    /**
     * Client secret của PaymentIntent — trả về FE để gọi stripe.confirmCardPayment().
     * KHÔNG log ra, chứa thông tin nhạy cảm.
     */
    @Column(name = "client_secret", columnDefinition = "TEXT")
    private String clientSecret;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private StripeOrderStatus status = StripeOrderStatus.PENDING;

    /** Charge ID Stripe trả về sau khi thanh toán thành công */
    @Column(name = "stripe_charge_id", length = 100)
    private String stripeChargeId;

    /** Thông điệp lỗi từ Stripe nếu thanh toán thất bại */
    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Thời điểm PaymentIntent hết hạn (Stripe tự hủy sau 24h, ta set 30 phút) */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
