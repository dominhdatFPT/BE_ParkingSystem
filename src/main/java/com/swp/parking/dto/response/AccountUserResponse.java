package com.swp.parking.dto.response;

import com.swp.parking.model.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountUserResponse {

    private Long userId;
    private String fullName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String status;
    private UserRole role;
    private LocalDateTime createdAt;
    private String cardStatus;
    private String feePackageName;
    private String licensePlate;

    @Builder.Default
    private List<AccountVehicleInfo> vehicles = new ArrayList<>();

    public AccountUserResponse(Long userId, String fullName, String email, String phone,
                               String avatarUrl, String status, UserRole role, LocalDateTime createdAt) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
        this.status = status;
        this.role = role;
        this.createdAt = createdAt;
    }
}
