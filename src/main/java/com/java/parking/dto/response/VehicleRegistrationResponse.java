package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleRegistrationResponse {

    private Long registrationId;

    private Long userId;

    private Long vehicleTypeId;

    private String vehicleTypeName;

    private String licensePlate;

    private String brand;

    private String color;

    private String status;

    private String rejectReason;

    private String ekycFullName;

    private String ekycCccdId;

    private String ekycLicenseNumber;

    private String ekycLicenseClass;

    private Boolean ekycIsValid;

    private Boolean ekycIsFake;

    private Double ekycConfidenceScore;

    private String cccdFrontImage;

    private String cccdBackImage;

    private String licenseImage;

    private String plateImage;

    private LocalDateTime createdAt;

    private LocalDateTime reviewedAt;
}
