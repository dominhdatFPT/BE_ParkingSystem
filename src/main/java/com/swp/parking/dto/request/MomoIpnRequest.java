package com.swp.parking.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * DTO ánh xạ payload IPN (Instant Payment Notification) mà MoMo POST đến server.
 * Tham khảo: https://developers.momo.vn/v3/docs/payment/api/payment-api/#ipn-callback
 */
@Data
public class MomoIpnRequest {

    private String partnerCode;
    private String orderId;
    private String requestId;
    private Long amount;
    private String orderInfo;
    private String orderType;
    private Long transId;
    private Integer resultCode;
    private String message;
    private String payType;
    private Long responseTime;
    private String extraData;

    /** Chữ ký HMAC-SHA256 do MoMo gửi — phải verify trước khi xử lý. */
    private String signature;

    /**
     * Partner token MoMo trả về sau khi thanh toán với requestType=payAndSendToken.
     * Dùng cho các lần charge recurring về sau.
     */
    @JsonProperty("callbackToken")
    private String callbackToken;
}
