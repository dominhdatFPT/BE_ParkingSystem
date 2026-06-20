package com.swp.parking.controller;

import com.swp.parking.dto.request.IncidentReplyRequest;
import com.swp.parking.service.SystemDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SystemDataController {
    private final SystemDataService service;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @GetMapping("/system-configuration")
    public ResponseEntity<Map<String, Object>> getConfiguration() {
        return ResponseEntity.ok(service.getConfiguration());
    }

    @PutMapping("/system-configuration")
    public ResponseEntity<Map<String, Object>> saveConfiguration(@RequestBody Map<String, Object> config) {
        return ResponseEntity.ok(service.saveConfiguration(currentUserId(), config));
    }

    @GetMapping("/incidents")
    public ResponseEntity<List<Map<String, Object>>> getIncidents() {
        return ResponseEntity.ok(service.getIncidents());
    }

    @PostMapping("/incidents")
    public ResponseEntity<Map<String, Object>> createIncident(@RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(service.createIncident(currentUserId(), request));
    }

    @PatchMapping("/incidents/{id}/reply")
    public ResponseEntity<Map<String, Object>> replyIncident(@PathVariable Long id,
            @Valid @RequestBody IncidentReplyRequest request) {
        return ResponseEntity.ok(service.replyIncident(currentUserId(), id, request));
    }

    @PatchMapping("/incidents/{id}/close")
    public ResponseEntity<Map<String, Object>> closeIncident(@PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request) {
        String resolution = request == null ? "Đã xử lý tại quầy" : request.getOrDefault("resolution", "Đã xử lý tại quầy");
        return ResponseEntity.ok(service.closeIncident(currentUserId(), id, resolution));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<List<Map<String, Object>>> getAuditLogs() {
        return ResponseEntity.ok(service.getAuditLogs());
    }
}
