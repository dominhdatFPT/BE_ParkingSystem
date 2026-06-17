package com.swp.parking.dto.request;

import com.swp.parking.model.enums.CardStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardStatusUpdateRequest {

    @NotNull(message = "Card status is required")
    private CardStatus status;
}
