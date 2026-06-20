package com.swp.parking.controller;

import com.swp.parking.dto.request.SupportRequest;
import com.swp.parking.service.SystemDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/support")
@RequiredArgsConstructor
public class CustomerSupportController {
    private final SystemDataService service;

    private Long currentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSupportRequest(@Valid @RequestBody SupportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createSupportRequest(currentUserId(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMySupportRequests() {
        return ResponseEntity.ok(service.getMySupportRequests(currentUserId()));
    }
}
