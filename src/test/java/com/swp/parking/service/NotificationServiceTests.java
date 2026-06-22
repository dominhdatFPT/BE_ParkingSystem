package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import com.swp.parking.model.Notification;
import com.swp.parking.model.enums.NotificationCategory;
import com.swp.parking.model.enums.NotificationRecipientTarget;
import com.swp.parking.model.enums.NotificationStatus;
import com.swp.parking.repository.DeviceTokenRepository;
import com.swp.parking.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTests {

    @Mock NotificationRepository notificationRepository;
    @Mock DeviceTokenRepository deviceTokenRepository;

    @InjectMocks NotificationService service;

    @Test
    void welcomeUsesOnlyPublicSentNotifications() {
        PageRequest pageable = PageRequest.of(0, 10);
        Notification publicNotification = Notification.builder()
                .id(1L)
                .category(NotificationCategory.HE_THONG)
                .title("Thông báo chung")
                .recipientTarget(NotificationRecipientTarget.ALL_USERS)
                .status(NotificationStatus.SENT)
                .build();
        when(notificationRepository.findPublicNotifications(NotificationStatus.SENT, pageable))
                .thenReturn(new PageImpl<>(List.of(publicNotification)));

        var result = service.getActiveNotifications(pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Thông báo chung", result.getContent().get(0).getTitle());
        verify(notificationRepository).findPublicNotifications(NotificationStatus.SENT, pageable);
    }

    @Test
    void publicDetailDoesNotExposeSpecificUserNotification() {
        Notification privateNotification = Notification.builder()
                .id(9L)
                .category(NotificationCategory.SU_CO)
                .title("Phản hồi sự cố")
                .isActive(true)
                .recipientTarget(NotificationRecipientTarget.SPECIFIC_USER)
                .recipientUserId(123L)
                .status(NotificationStatus.SENT)
                .build();
        when(notificationRepository.findById(9L)).thenReturn(Optional.of(privateNotification));

        assertThrows(AppException.class, () -> service.getNotificationDetail(9L));
    }
}
