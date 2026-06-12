package com.swp.parking.dto.response;

import com.swp.parking.model.enums.ParkingSlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotInfoResponse {

    private Long id;
    private String slotNumber;
    private ParkingSlotStatus status;
}
