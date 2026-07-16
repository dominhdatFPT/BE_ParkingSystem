package com.swp.parking.dto.request;

import com.swp.parking.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    @NotBlank(message = "Mật khẩu hiện tại không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(regexp = ValidationPatterns.STRONG_PASSWORD,
            message = "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ và số")
    private String newPassword;
}
