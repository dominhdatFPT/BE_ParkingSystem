package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminReviewRequest {

    @NotBlank
    private String status;

    private String rejectReason;
}
