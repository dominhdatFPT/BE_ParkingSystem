package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VNPayOrderStatusResponse {

    private String txnRef;

    /** PENDING | PAID | FAILED | CANCELLED */
    private String status;

    private Long amount;
    private String orderInfo;
    private String vnpTransactionNo;
    private String vnpResponseCode;
    private String vnpBankCode;

    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;

    /** true nếu đơn đã quá hạn mà chưa thanh toán */
    private boolean expired;
}
