package com.swp.parking.model;

import com.swp.parking.model.enums.MomoOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "momo_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MomoOrder {

    @Id
    @Column(name = "order_id", nullable = false, unique = true, length = 50)
    private String orderId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal amount;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "momo_account", length = 20)
    private String momoAccount;

    @Column(name = "momo_name", length = 100)
    private String momoName;

    // Base64 PNG của ảnh QR code (data:image/png;base64,...)
    @Column(name = "qr_code_data", columnDefinition = "TEXT")
    private String qrCodeData;

    @Column(name = "qr_code_image_url", length = 500)
    private String qrCodeImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private MomoOrderStatus paymentStatus = MomoOrderStatus.PENDING;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "notes", length = 500)
    private String notes;
}
