package com.swp.parking.model;

import com.swp.parking.model.enums.VNPayOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vnpay_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VNPayOrder {

    // Mã tham chiếu giao dịch gửi lên VNPay (unique)
    @Id
    @Column(name = "txn_ref", nullable = false, unique = true, length = 50)
    private String txnRef;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "subscription_id")
    private Long subscriptionId;

    @Column(name = "invoice_id")
    private Long invoiceId;

    // Số tiền thực tế (VNĐ) — không nhân 100 như khi gửi lên VNPay
    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "order_info", length = 500)
    private String orderInfo;

    // Link thanh toán VNPay trả về khi tạo đơn
    @Column(name = "payment_url", columnDefinition = "TEXT")
    private String paymentUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VNPayOrderStatus status = VNPayOrderStatus.PENDING;

    // Mã giao dịch VNPay trả về trong IPN / Return URL
    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;

    // Response code VNPay: "00" = thành công
    @Column(name = "vnp_response_code", length = 10)
    private String vnpResponseCode;

    // Ngân hàng thực hiện giao dịch
    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Thời điểm đơn hết hạn (mặc định 15 phút)
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
