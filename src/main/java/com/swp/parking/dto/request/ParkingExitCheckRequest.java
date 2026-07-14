package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParkingExitCheckRequest {

    @NotBlank(message = "Bien so xe khong duoc de trong")
    @Size(max = 20, message = "Bien so xe toi da 20 ky tu")
    private String licensePlate;
}
