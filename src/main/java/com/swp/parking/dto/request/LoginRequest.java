package com.swp.parking.dto.request;

import com.swp.parking.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = ValidationPatterns.GMAIL_EMAIL,
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Email must be a Gmail address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    private boolean rememberMe;
}
