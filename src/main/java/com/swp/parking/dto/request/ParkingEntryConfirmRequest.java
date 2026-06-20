package com.swp.parking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingEntryConfirmRequest {

    private String licensePlate;

    private String vehicleType;

    private String visitorCardCode;
}
