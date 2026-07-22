package com.swp.parking.dto.request;

import com.swp.parking.validation.ValidationPatterns;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountPhoneRequest {

    @Pattern(
            regexp = ValidationPatterns.OPTIONAL_VIETNAM_PHONE,
            message = "Phone number must be 10-11 digits and start with 0")
    private String phone;
}
