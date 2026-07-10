package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class VisitorCheckoutResponse {
    private Long orderId;
    private String orderCode;
    private String licensePlate;
    private String vehicleType;
    private String visitorCardCode;
    private LocalDateTime entryTime;
    private Long durationMinutes;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String paymentMethod;
    private BigDecimal paidAmount;
    private LocalDateTime paidAt;
    private String paymentIntentId;
    private String clientSecret;
    private String message;
}
