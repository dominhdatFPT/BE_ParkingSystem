package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveParkingOrderResponse {

    private Long orderId;
    private String status;
    private String licensePlate;
    private String vehicleType;
    private String facilityName;
    private String facilityAddress;
    private String floorName;
    private String slotNumber;
    private LocalDateTime entryTime;
    private Long durationMinutes;
    private Double currentFee;
}
