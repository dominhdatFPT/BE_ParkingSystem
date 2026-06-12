package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAreaSummaryResponse {

    private Long id;
    private String buildingCode;
    private String buildingName;
    private Integer floorNumber;
    private String areaCode;
    private String vehicleType;
    private Integer capacity;
    private Integer currentVehicleCount;
    private Integer occupancyPercent;
}
