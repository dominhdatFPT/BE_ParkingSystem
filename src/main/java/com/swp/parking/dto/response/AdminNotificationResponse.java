package com.swp.parking.dto.response;

import com.swp.parking.model.enums.NotificationCategory;
import com.swp.parking.model.enums.NotificationPriority;
import com.swp.parking.model.enums.NotificationRecipientTarget;
import com.swp.parking.model.enums.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminNotificationResponse {

    private Long id;
    private String title;
    private String content;
    private NotificationCategory type;
    private NotificationRecipientTarget target;
    private NotificationPriority priority;
    private NotificationStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long createdBy;
    private Boolean isActive;
}
