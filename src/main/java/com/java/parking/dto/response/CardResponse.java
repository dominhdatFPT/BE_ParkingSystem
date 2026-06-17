package com.swp.parking.dto.response;

import com.swp.parking.model.enums.CardStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardResponse {

    private Long id;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private String cardCode;
    private CardStatus status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
