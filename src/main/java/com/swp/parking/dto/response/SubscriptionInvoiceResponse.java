package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionInvoiceResponse {

    private Long id;

    // ── VNPay (kênh thanh toán hiện tại) ────────────────────────────
    private String vnpTxnRef;
    private String vnpTransactionNo;

    // ── MoMo (legacy — dữ liệu cũ) ──────────────────────────────────
    private String momoOrderId;
    private Long momoTransId;

    // ── Stripe ───────────────────────────────────────────────────────
    private String stripePaymentIntentId;

    private BigDecimal amount;

    /** PENDING | SUCCESS | FAILED */
    private String status;

    /** INITIAL | RENEWAL */
    private String type;

    private String message;
    private String licensePlate;
    private String planName;
    private LocalDateTime createdAt;
}
