package com.swp.parking.service;

import com.swp.parking.dto.request.RegisterDeviceTokenRequest;
import com.swp.parking.dto.response.CustomerNotificationResponse;
import com.swp.parking.dto.response.NotificationDetailResponse;
import com.swp.parking.dto.response.NotificationListItemResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.DeviceToken;
import com.swp.parking.model.Notification;
import com.swp.parking.model.enums.NotificationCategory;
import com.swp.parking.model.enums.NotificationPriority;
import com.swp.parking.model.enums.NotificationRecipientTarget;
import com.swp.parking.model.enums.NotificationStatus;
import com.swp.parking.repository.DeviceTokenRepository;
import com.swp.parking.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
                .findPublicNotifications(NotificationStatus.SENT, pageable)
                .map(this::toListItem);
    }

    @Transactional(readOnly = true)
    public List<CustomerNotificationResponse> getCustomerNotifications(Long userId) {
        return notificationRepository
                .findVisibleForUser(userId, NotificationStatus.SENT)
                .stream()
                .map(this::toCustomerResponse)
                .toList();
    }

    public void createIncidentReplyNotification(Long recipientUserId, Long staffUserId,
                                                String replyTitle, String replyMessage) {
        if (recipientUserId == null) return;
        Notification notification = Notification.builder()
                .category(NotificationCategory.SU_CO)
                .title("Phản hồi từ Admin: " + replyTitle)
                .summary(replyMessage)
                .content(replyMessage)
                .isActive(true)
                .priority(NotificationPriority.IMPORTANT)
                .recipientTarget(NotificationRecipientTarget.SPECIFIC_USER)
                .recipientUserId(recipientUserId)
                .status(NotificationStatus.SENT)
                .publishedAt(LocalDateTime.now())
                .createdBy(staffUserId)
                .build();
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public NotificationDetailResponse getNotificationDetail(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (Boolean.FALSE.equals(notification.getIsActive())
                || notification.getStatus() != NotificationStatus.SENT
                || notification.getRecipientUserId() != null
                || notification.getRecipientTarget() == NotificationRecipientTarget.SPECIFIC_USER) {
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

    public void markCustomerNotificationRead(Long id) {
        // TODO: persist per-user read state when a user_notifications table is added
        log.debug("Mark notification read requested for id: {}", id);
    }

    public void markAllCustomerNotificationsRead() {
        // TODO: persist per-user read state when a user_notifications table is added
        log.debug("Mark all notifications read requested");
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

    private CustomerNotificationResponse toCustomerResponse(Notification notification) {
        String message = notification.getSummary();
        if (message == null || message.isBlank()) {
            message = notification.getContent();
        }
        if (message == null || message.isBlank()) {
            message = notification.getTitle();
        }
        return CustomerNotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(message)
                .content(notification.getContent())
                .time(notification.getPublishedAt() != null ? notification.getPublishedAt() : notification.getCreatedAt())
                .type(mapCategoryToType(notification.getCategory()))
                .read(false)
                .build();
    }

    private String mapCategoryToType(NotificationCategory category) {
        if (category == null) return "info";
        return switch (category) {
            case SU_CO -> "error";
            case BAO_TRI -> "warning";
            case THANH_TOAN, GOI_GUI_XE -> "success";
            default -> "info";
        };
    }
}
