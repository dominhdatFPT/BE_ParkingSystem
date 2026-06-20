package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class MomoOrderStatusResponse {

    private String orderId;

    /** PENDING | PAID | FAILED | CANCELLED */
    private String paymentStatus;

    private String momoAccount;
    private String momoName;
    private BigDecimal amount;
    private String description;

    /** Ảnh QR dạng Base64 PNG – dùng trực tiếp: <img src="{qrCodeData}" /> */
    private String qrCodeData;

    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    /** true nếu đơn đã quá 10 phút mà chưa thanh toán */
    private boolean expired;

    private String notes;
}
