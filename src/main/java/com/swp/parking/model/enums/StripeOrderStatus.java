package com.swp.parking.model.enums;

public enum StripeOrderStatus {
    /** PaymentIntent đã tạo, chờ user nhập thẻ */
    PENDING,
    /** Stripe xác nhận thanh toán thành công (payment_intent.succeeded) */
    SUCCEEDED,
    /** Thanh toán thất bại (payment_intent.payment_failed) */
    FAILED,
    /** Đơn bị hủy thủ công hoặc hết hạn */
    CANCELLED
}
