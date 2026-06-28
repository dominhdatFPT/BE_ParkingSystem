package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {

    @NotBlank(message = "Role không được để trống")
    @Pattern(regexp = "^(USER|STAFF|ADMIN)$", message = "Role chỉ chấp nhận USER, STAFF, ADMIN")
    private String role;
}
