package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO trả về thông tin xe đang gửi và vị trí đỗ cho khách hàng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInfoResponse {

    private String licensePlate;
    private String brand;
    private String color;
    private String vehicleTypeName;
    private String parkingStatus;
    private String parkingName;
    private String floorName;
    private Integer floorNumber;
    private LocalDateTime entryTime;
}
