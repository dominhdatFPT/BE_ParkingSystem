package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterSubscriptionStripeResponse {

    private Long subscriptionId;
    private Long invoiceId;

    /** ID của Stripe PaymentIntent (pi_xxx) — dùng để polling trạng thái */
    private String paymentIntentId;

    /**
     * Client secret của PaymentIntent — FE dùng để gọi stripe.confirmCardPayment().
     * Chỉ tồn tại trong response này, KHÔNG lưu bền vào DB phía FE.
     */
    private String clientSecret;

    /** Số tiền cần thanh toán */
    private Long amount;

    /** Mã tiền tệ (vd: "vnd") */
    private String currency;

    private String message;
}
