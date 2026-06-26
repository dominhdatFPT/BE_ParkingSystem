package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisterSubscriptionResponse {

    private Long subscriptionId;
    private Long invoiceId;

    /** Mã tham chiếu giao dịch VNPay (vnp_TxnRef) – dùng để polling trạng thái */
    private String vnpTxnRef;

    /**
     * Link thanh toán VNPay — FE redirect trực tiếp: window.location.href = paymentUrl
     * User sẽ được đưa sang trang VNPay để chọn ngân hàng / quét QR.
     */
    private String paymentUrl;

    /** Thời điểm đơn hàng hết hạn (mặc định 15 phút sau khi tạo) */
    private LocalDateTime expiredAt;

    private String message;
}
