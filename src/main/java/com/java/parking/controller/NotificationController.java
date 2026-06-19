package com.swp.parking.controller;

import com.swp.parking.dto.request.RegisterDeviceTokenRequest;
import com.swp.parking.dto.response.NotificationDetailResponse;
import com.swp.parking.dto.response.NotificationListItemResponse;
import com.swp.parking.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<Page<NotificationListItemResponse>> getActiveNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(notificationService.getActiveNotifications(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getNotificationDetail(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationDetail(id));
    }

    @PostMapping("/register-token")
    public ResponseEntity<Void> registerDeviceToken(@Valid @RequestBody RegisterDeviceTokenRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        notificationService.registerDeviceToken(userId, request);
        return ResponseEntity.noContent().build();
    }
}
