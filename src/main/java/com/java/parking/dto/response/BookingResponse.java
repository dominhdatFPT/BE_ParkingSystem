package com.swp.parking.dto.response;

import com.swp.parking.model.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private Long parkingSlotId;
    private String slotNumber;
    private Long parkingId;
    private String parkingName;
    private Long floorId;
    private String floorName;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private Long zoneId;
    private String zoneName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private Long acceptedBy;
    private String acceptedByName;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
