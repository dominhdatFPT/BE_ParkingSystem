package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi sau khi đăng nhập thành công, kèm JWT và thông tin người dùng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** JWT access token */
    private String token;

    /** Loại token, mặc định Bearer */
    @Builder.Default
    private String tokenType = "Bearer";

    /** ID tài khoản users */
    private Long userId;

    /** Họ tên đầy đủ */
    private String fullName;

    /** Email đăng nhập */
    private String email;

    /** Vai trò: CUSTOMER / SECURITY / CASHIER / ADMIN */
    private String role;

    /** ID customer (null nếu là employee) */
    private Long customerId;

    /** ID employee (null nếu là customer) */
    private Long employeeId;
}
