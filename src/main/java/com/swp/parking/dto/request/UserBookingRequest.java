package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBookingRequest {

    @NotNull(message = "Parking facility ID is required")
    private Long parkingId;

    @NotNull(message = "Floor ID is required")
    private Long floorId;

    @NotNull(message = "Vehicle type ID is required")
    private Long vehicleTypeId;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;
}
