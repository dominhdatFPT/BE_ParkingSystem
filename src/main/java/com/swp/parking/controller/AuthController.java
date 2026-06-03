package com.swp.parking.controller;

import com.swp.parking.dto.auth.SessionStatusResponse;
import com.swp.parking.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/session")
    public ResponseEntity<SessionStatusResponse> getSessionStatus() {
        return ResponseEntity.ok(authService.getSessionStatus());
    }
}
