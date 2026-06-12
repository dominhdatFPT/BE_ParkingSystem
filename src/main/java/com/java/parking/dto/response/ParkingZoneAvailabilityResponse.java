package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingZoneAvailabilityResponse {

    private Long parkingId;
    private String parkingName;
    private Long floorId;
    private String floorName;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private Long zoneId;
    private String zoneName;
    private Integer totalSlots;
    private Long activeVehicleCount;
    private Long pendingBookingCount;
    private Long confirmedBookingCount;
    private Long availableSlots;
}
