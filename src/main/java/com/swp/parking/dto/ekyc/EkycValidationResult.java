package com.swp.parking.dto.ekyc;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EkycValidationResult {

    @JsonProperty("is_valid")
    private Boolean isValid;

    @JsonProperty("is_fake")
    private Boolean isFake;

    @JsonProperty("document_type")
    private String documentType;

    @JsonProperty("confidence_score")
    private Double confidenceScore;
}
