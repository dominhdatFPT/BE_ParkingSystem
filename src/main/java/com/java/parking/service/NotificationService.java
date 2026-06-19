package com.swp.parking.service;

import com.swp.parking.dto.request.RegisterDeviceTokenRequest;
import com.swp.parking.dto.response.NotificationDetailResponse;
import com.swp.parking.dto.response.NotificationListItemResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.DeviceToken;
import com.swp.parking.model.Notification;
import com.swp.parking.repository.DeviceTokenRepository;
import com.swp.parking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional(readOnly = true)
    public Page<NotificationListItemResponse> getActiveNotifications(Pageable pageable) {
        return notificationRepository
                .findByIsActiveTrueOrderByPublishedAtDesc(pageable)
                .map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (Boolean.FALSE.equals(notification.getIsActive())) {
            throw new AppException(HttpStatus.NOT_FOUND, "Notification not found");
        }

        return toDetail(notification);
    }

    public void registerDeviceToken(Long userId, RegisterDeviceTokenRequest request) {
        String token = request.getToken();
        DeviceToken existing = deviceTokenRepository.findByToken(token).orElse(null);

        if (existing != null) {
            existing.setUserId(userId);
            existing.setPlatform(request.getPlatform());
            deviceTokenRepository.save(existing);
            return;
        }

        DeviceToken newToken = DeviceToken.builder()
                .userId(userId)
                .token(token)
                .platform(request.getPlatform())
                .build();
        deviceTokenRepository.save(newToken);
    }

    private NotificationListItemResponse toListItem(Notification notification) {
        return NotificationListItemResponse.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .title(notification.getTitle())
                .summary(notification.getSummary())
                .publishedAt(notification.getPublishedAt())
                .build();
    }

    private NotificationDetailResponse toDetail(Notification notification) {
        return NotificationDetailResponse.builder()
                .id(notification.getId())
                .category(notification.getCategory())
                .title(notification.getTitle())
                .summary(notification.getSummary())
                .content(notification.getContent())
                .publishedAt(notification.getPublishedAt())
                .build();
    }
}
