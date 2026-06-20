package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {

    private Long id;

    private Long vehicleId;
    private String licensePlate;

    private Long planId;
    private String planName;
    private Integer durationMonths;

    private BigDecimal amountToPay;

    /** Giá trị: PENDING_PAYMENT | ACTIVE | CANCELLED | EXPIRED */
    private String status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Boolean isAutoRenew;

    private LocalDateTime createdAt;
}
