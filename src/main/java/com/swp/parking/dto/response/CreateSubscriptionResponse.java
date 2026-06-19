package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSubscriptionResponse {

    private Long subscriptionId;
    private String status;
    private BigDecimal amountToPay;
    private String feePackageName;
    private String vehicleLicensePlate;
    private LocalDateTime createdAt;
}
