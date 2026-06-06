package com.swp.parking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO nhận thông tin đăng nhập từ client (email + mật khẩu).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    /** Email đăng nhập */
    private String email;

    /** Mật khẩu dạng plain text (sẽ so khớp với BCrypt hash trong DB) */
    private String password;
}
