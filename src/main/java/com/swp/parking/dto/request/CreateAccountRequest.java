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
public class CreateAccountRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Pattern(
            regexp = ValidationPatterns.GMAIL_EMAIL,
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Email must be a Gmail address")
    private String email;

    @Pattern(
            regexp = ValidationPatterns.OPTIONAL_VIETNAM_PHONE,
            message = "Phone number must be 10-11 digits and start with 0")
    private String phone;

    @NotBlank(message = "Password is required")
    @Pattern(
            regexp = ValidationPatterns.STRONG_PASSWORD,
            message = "Password must be at least 8 characters and contain at least one letter and one number")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "^(USER|STAFF|ADMIN)$", message = "Role must be USER, STAFF, or ADMIN")
    private String role;
}
