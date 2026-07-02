package com.swp.parking.dto.request;

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

    private Long requestedFeePackageId;

    private Boolean feePackagePaidCash;

    private String licensePlate;

    private String brand;

    private String color;

    private String cccdFrontImage;

    private String cccdBackImage;

    private String licenseImage;

    private String vehicleDocumentImage;

    private String plateImage;
}
