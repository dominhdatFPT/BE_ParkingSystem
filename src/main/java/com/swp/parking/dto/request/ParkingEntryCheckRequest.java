package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingEntryCheckRequest {

    @NotBlank(message = "Bien so xe khong duoc de trong")
    @Size(max = 20, message = "Bien so xe toi da 20 ky tu")
    private String licensePlate;

    private String vehicleType;
}
