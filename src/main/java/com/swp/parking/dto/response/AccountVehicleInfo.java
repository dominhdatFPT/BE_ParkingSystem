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
public class AccountVehicleInfo {

    private Long vehicleId;
    private String licensePlate;
    private String brand;
    private String color;
    private String vehicleTypeName;
    private String vehicleTypeCode;

    private Long subscriptionId;
    private String subscriptionStatus;
    private BigDecimal amountToPay;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private String paymentStatus;
    private String paymentStatusLabel;

    private String feePackageName;
}
