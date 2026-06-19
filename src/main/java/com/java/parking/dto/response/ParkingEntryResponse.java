package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingEntryResponse {

    private String entryType;

    private Boolean registered;

    private Boolean hasActiveSubscription;

    private Long vehicleId;

    private String customerName;

    private String vehicleBrand;

    private String vehicleColor;

    private Long orderId;

    private String licensePlate;

    private String vehicleType;

    private String monthlyPackageName;

    private LocalDateTime subscriptionEndDate;

    private String visitorCardCode;

    private LocalDateTime entryTime;

    private String sessionStatus;

    private Boolean canConfirm;

    private String message;
}
