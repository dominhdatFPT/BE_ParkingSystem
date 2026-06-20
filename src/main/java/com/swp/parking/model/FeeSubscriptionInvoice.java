package com.swp.parking.model;

import com.swp.parking.model.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "fee_subscription_invoice")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeeSubscriptionInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "invoice_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fee_subscription_id", nullable = false)
    private FeeSubscription feeSubscription;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    @Column(name = "momo_order_id", unique = true)
    private String momoOrderId;

    @Column(name = "momo_request_id")
    private String momoRequestId;

    @Column(name = "momo_trans_id")
    private Long momoTransId;

    @Column(name = "message", length = 512)
    private String message;

    /** INITIAL = kỳ đầu | RENEWAL = gia hạn */
    @Column(name = "type", length = 20)
    private String type;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
