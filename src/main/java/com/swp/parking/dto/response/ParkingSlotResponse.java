package com.swp.parking.dto.response;

import com.swp.parking.model.enums.ParkingSlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlotResponse {

    private Long id;
    private String slotNumber;
    private Integer floor;
    private ParkingSlotStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
