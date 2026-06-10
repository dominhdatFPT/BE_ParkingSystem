package com.swp.parking.dto.request;

import com.swp.parking.model.enums.ParkingSlotStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingSlotRequest {

    @NotBlank(message = "Slot number is required")
    private String slotNumber;

    @NotNull(message = "Floor is required")
    private Integer floor;

    @NotNull(message = "Status is required")
    private ParkingSlotStatus status;
}
