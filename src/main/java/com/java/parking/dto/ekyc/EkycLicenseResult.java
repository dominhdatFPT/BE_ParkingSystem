package com.swp.parking.dto.ekyc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkycLicenseResult {

    @JsonProperty("license_number")
    private String licenseNumber;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("license_class")
    private String licenseClass;

    @JsonProperty("issue_date")
    private String issueDate;

    @JsonProperty("expiry_date")
    private String expiryDate;

    @JsonProperty("issuing_authority")
    private String issuingAuthority;
}
