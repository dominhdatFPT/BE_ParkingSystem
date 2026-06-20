package com.swp.parking.dto.response;

import com.swp.parking.model.enums.NotificationCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListItemResponse {

    private Long id;
    private NotificationCategory category;
    private String title;
    private String summary;
    private LocalDateTime publishedAt;
}
