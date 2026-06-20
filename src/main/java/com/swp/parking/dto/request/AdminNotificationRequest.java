package com.swp.parking.dto.request;

import com.swp.parking.model.enums.NotificationCategory;
import com.swp.parking.model.enums.NotificationPriority;
import com.swp.parking.model.enums.NotificationRecipientTarget;
import com.swp.parking.model.enums.NotificationStatus;
import jakarta.validation.constraints.NotBlank;
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
public class AdminNotificationRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;

    @NotNull(message = "Type/category is required")
    private NotificationCategory type;

    @NotNull(message = "Recipient target is required")
    private NotificationRecipientTarget target;

    @NotNull(message = "Priority is required")
    private NotificationPriority priority;

    @NotNull(message = "Status is required")
    private NotificationStatus status;

    private LocalDateTime scheduledAt;
}
