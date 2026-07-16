package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionInvoiceResponse {

    private Long id;

    private Long subscriptionId;

    // ── MoMo (legacy — dữ liệu cũ) ──────────────────────────────────
    private String momoOrderId;
    private Long momoTransId;

    // ── Stripe ───────────────────────────────────────────────────────
    private String stripePaymentIntentId;

    private BigDecimal amount;

    /** PENDING | SUCCESS | FAILED */
    private String status;

    private String subscriptionStatus;

    private Boolean payable;

    /** INITIAL | RENEWAL */
    private String type;

    private String message;
    private String licensePlate;
    private String planName;
    private LocalDateTime createdAt;
}
