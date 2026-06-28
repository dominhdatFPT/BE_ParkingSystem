package com.swp.parking.dto.response;

import com.swp.parking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String tokenType;
    private Long userId;
    private String fullName;
    private String email;
    private UserRole role;
    private String refreshToken;
}
