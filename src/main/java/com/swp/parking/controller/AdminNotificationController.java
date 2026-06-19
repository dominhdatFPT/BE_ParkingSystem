package com.swp.parking.controller;

import com.swp.parking.dto.request.AdminNotificationRequest;
import com.swp.parking.dto.response.AdminNotificationResponse;
import com.swp.parking.service.AdminNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    @GetMapping
    public ResponseEntity<List<AdminNotificationResponse>> getAllNotifications() {
        return ResponseEntity.ok(adminNotificationService.getAllNotifications());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminNotificationResponse> getNotificationById(@PathVariable Long id) {
        return ResponseEntity.ok(adminNotificationService.getNotificationById(id));
    }

    @PostMapping
    public ResponseEntity<AdminNotificationResponse> createNotification(
            @Valid @RequestBody AdminNotificationRequest request) {
        Long createdBy = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(adminNotificationService.createNotification(createdBy, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminNotificationResponse> updateNotification(
            @PathVariable Long id,
            @Valid @RequestBody AdminNotificationRequest request) {
        return ResponseEntity.ok(adminNotificationService.updateNotification(id, request));
    }

    @PatchMapping("/{id}/send")
    public ResponseEntity<AdminNotificationResponse> sendNotification(@PathVariable Long id) {
        return ResponseEntity.ok(adminNotificationService.sendNotification(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        adminNotificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }
}
