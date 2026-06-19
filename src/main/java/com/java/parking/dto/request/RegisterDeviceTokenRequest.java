package com.swp.parking.dto.request;

import com.swp.parking.model.enums.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "token is required")
    @Size(max = 500, message = "token must not exceed 500 characters")
    private String token;

    @NotNull(message = "platform is required")
    private Platform platform;
}
