package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRegistrationRequest {

    @NotNull
    private Long vehicleTypeId;

    private String licensePlate;

    private String brand;

    private String color;

    @NotBlank
    private String cccdFrontImage;

    @NotBlank
    private String cccdBackImage;

    @NotBlank
    private String licenseImage;

    @NotBlank
    private String vehicleDocumentImage;

    @NotBlank
    private String plateImage;
}
