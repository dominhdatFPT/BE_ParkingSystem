package com.swp.parking.controller;

import com.swp.parking.dto.response.CustomerNotificationResponse;
import com.swp.parking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerNotificationController {

    private final NotificationService notificationService;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<CustomerNotificationResponse>> getCustomerNotifications() {
        return ResponseEntity.ok(notificationService.getCustomerNotifications(currentUserId()));
    }

    @PatchMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markNotificationRead(@PathVariable Long id) {
        notificationService.markCustomerNotificationRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllNotificationsRead() {
        notificationService.markAllCustomerNotificationsRead();
        return ResponseEntity.noContent().build();
    }
}
