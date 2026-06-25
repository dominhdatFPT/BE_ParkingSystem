package com.swp.parking.model.enums;

public enum VNPayOrderStatus {
    PENDING,   // Chờ user thanh toán
    PAID,      // VNPay xác nhận thành công
    FAILED,    // Thanh toán thất bại / bị từ chối
    CANCELLED  // Hết hạn hoặc user hủy
}
