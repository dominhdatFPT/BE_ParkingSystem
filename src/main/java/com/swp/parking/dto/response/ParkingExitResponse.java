package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingExitResponse {
    private Long orderId;
    private String orderCode;
    private String exitType;
    private String licensePlate;
    private String vehicleType;
    private String brand;
    private String color;
    private String customerName;
    private String visitorCardCode;
    private LocalDateTime entryTime;
    private LocalDateTime exitTime;
    private Long durationMinutes;
    private String parkingStatus;
    private SubscriptionInfo subscription;
    private FeeInfo fee;
    private Boolean canConfirmExit;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionInfo {
        private Long subscriptionId;
        private String packageName;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeeInfo {
        private Boolean required;
        private Long feeRateId;
        private BigDecimal amount;
        private String currency;
        private String description;
        private Integer firstBlockMinutes;
        private BigDecimal firstBlockFee;
        private Integer nextBlockMinutes;
        private BigDecimal nextBlockFee;
        private Integer additionalBlocks;
        private BigDecimal additionalFee;
        private BigDecimal dailyCap;
    }
}
