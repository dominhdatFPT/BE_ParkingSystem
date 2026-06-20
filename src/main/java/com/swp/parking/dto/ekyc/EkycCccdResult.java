package com.swp.parking.dto.ekyc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkycCccdResult {

    @JsonProperty("id")
    private String id;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("date_of_birth")
    private String dateOfBirth;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("nationality")
    private String nationality;

    @JsonProperty("place_of_origin")
    private String placeOfOrigin;

    @JsonProperty("place_of_residence")
    private String placeOfResidence;

    @JsonProperty("issue_date")
    private String issueDate;

    @JsonProperty("expiry_date")
    private String expiryDate;

    @JsonProperty("confidence")
    private Double confidence;
}
