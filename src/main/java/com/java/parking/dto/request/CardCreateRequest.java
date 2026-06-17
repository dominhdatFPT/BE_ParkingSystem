package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardCreateRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private LocalDateTime expiredAt;
}
