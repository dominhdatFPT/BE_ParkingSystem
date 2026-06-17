package com.swp.parking.controller;

import com.swp.parking.dto.request.CardCreateRequest;
import com.swp.parking.dto.request.CardStatusUpdateRequest;
import com.swp.parking.dto.response.CardResponse;
import com.swp.parking.model.enums.CardStatus;
import com.swp.parking.service.CardService;
import com.swp.parking.service.SecurityRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final SecurityRoleService securityRoleService;

    @GetMapping("/my-card")
    public ResponseEntity<CardResponse> getMyCard() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(cardService.getOrCreateMyCard(userId));
    }

    @GetMapping
    public ResponseEntity<List<CardResponse>> getAllCards(@RequestParam(required = false) CardStatus status) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(cardService.getAllCards(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(@PathVariable Long id) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @PostMapping
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CardCreateRequest request) {
        securityRoleService.requireAnyRole("ADMIN");
        return ResponseEntity.status(HttpStatus.CREATED).body(cardService.createCard(request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CardResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody CardStatusUpdateRequest request) {
        securityRoleService.requireAnyRole("ADMIN");
        return ResponseEntity.ok(cardService.updateStatus(id, request));
    }
}
