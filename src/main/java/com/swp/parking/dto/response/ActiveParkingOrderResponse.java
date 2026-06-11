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
public class ActiveParkingOrderResponse {

    private Long orderId;
    private String licensePlate;
    private String vehicleTypeName;
    private String parkingName;
    private String floorName;
    private LocalDateTime entryTime;
    private BigDecimal calculatedFee;
    private String parkingStatus;
}
