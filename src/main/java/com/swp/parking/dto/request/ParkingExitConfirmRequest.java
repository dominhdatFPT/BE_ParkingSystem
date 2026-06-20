package com.swp.parking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingExitConfirmRequest {
    private Boolean paymentConfirmed;
    private String paymentMethod;
}
