package com.swp.parking.service;

import com.swp.parking.dto.request.AdminNotificationRequest;
import com.swp.parking.dto.response.AdminNotificationResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.Notification;
import com.swp.parking.model.enums.NotificationStatus;
import com.swp.parking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminNotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<AdminNotificationResponse> getAllNotifications() {
        return notificationRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminNotificationResponse getNotificationById(Long id) {
        Notification notification = findById(id);
        return toResponse(notification);
    }

    public AdminNotificationResponse createNotification(Long createdBy, AdminNotificationRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Notification notification = Notification.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getType())
                .recipientTarget(request.getTarget())
                .priority(request.getPriority())
                .status(request.getStatus())
                .scheduledAt(request.getScheduledAt())
                .createdBy(createdBy)
                .isActive(true)
                .build();

        if (request.getStatus() == NotificationStatus.SENT) {
            notification.setPublishedAt(now);
        }

        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    public AdminNotificationResponse updateNotification(Long id, AdminNotificationRequest request) {
        Notification notification = findById(id);

        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setCategory(request.getType());
        notification.setRecipientTarget(request.getTarget());
        notification.setPriority(request.getPriority());
        notification.setStatus(request.getStatus());
        notification.setScheduledAt(request.getScheduledAt());

        if (request.getStatus() == NotificationStatus.SENT && notification.getPublishedAt() == null) {
            notification.setPublishedAt(LocalDateTime.now());
        }

        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    public AdminNotificationResponse sendNotification(Long id) {
        Notification notification = findById(id);
        notification.setStatus(NotificationStatus.SENT);
        notification.setPublishedAt(LocalDateTime.now());
        notification.setIsActive(true);
        Notification saved = notificationRepository.save(notification);
        return toResponse(saved);
    }

    public void deleteNotification(Long id) {
        Notification notification = findById(id);
        notificationRepository.delete(notification);
    }

    private Notification findById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private AdminNotificationResponse toResponse(Notification notification) {
        return AdminNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getCategory())
                .target(notification.getRecipientTarget())
                .priority(notification.getPriority())
                .status(notification.getStatus())
                .scheduledAt(notification.getScheduledAt())
                .publishedAt(notification.getPublishedAt())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getUpdatedAt())
                .createdBy(notification.getCreatedBy())
                .isActive(notification.getIsActive())
                .build();
    }
}
