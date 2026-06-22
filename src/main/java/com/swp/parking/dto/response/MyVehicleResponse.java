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
public class MyVehicleResponse {

    private Long vehicleId;
    private String licensePlate;
    private String brand;
    private String color;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String vehicleTypeCode;
    private LocalDateTime createdAt;
}
