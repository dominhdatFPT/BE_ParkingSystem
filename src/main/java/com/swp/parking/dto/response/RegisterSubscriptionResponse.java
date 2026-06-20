package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisterSubscriptionResponse {

    private Long subscriptionId;
    private Long invoiceId;

    /** ID đơn hàng MoMo – dùng để polling trạng thái thanh toán */
    private String momoOrderId;

    /** Số điện thoại MoMo nhận tiền */
    private String momoAccount;

    /** Tên chủ tài khoản MoMo */
    private String momoName;

    /**
     * Ảnh QR code dạng Base64 PNG (data:image/png;base64,...).
     * Frontend hiển thị trực tiếp: <img src="{qrCodeData}" />
     * Khi quét bằng app MoMo, màn hình chuyển tiền tự điền sẵn tài khoản + số tiền.
     */
    private String qrCodeData;

    /** Thời điểm đơn hàng hết hạn (mặc định 10 phút sau khi tạo) */
    private LocalDateTime expiredAt;

    private String message;
}
