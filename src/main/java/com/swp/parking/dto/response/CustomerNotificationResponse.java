package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNotificationResponse {
    private Long id;
    private String title;
    private String message;
    private String content;
    private LocalDateTime time;
    private String type;
    private boolean read;
}
