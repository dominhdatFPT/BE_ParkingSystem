package com.swp.parking.controller;

import com.swp.parking.dto.request.ParkingExitCheckRequest;
import com.swp.parking.dto.request.ParkingExitConfirmRequest;
import com.swp.parking.dto.response.ParkingExitResponse;
import com.swp.parking.service.ParkingExitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parking-exit")
@RequiredArgsConstructor
public class ParkingExitController {

    private final ParkingExitService parkingExitService;

    @PostMapping("/check")
    public ResponseEntity<ParkingExitResponse> checkVehicle(@Valid @RequestBody ParkingExitCheckRequest request) {
        return ResponseEntity.ok(parkingExitService.checkVehicle(request));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ParkingExitResponse> confirmExit(
            @PathVariable Long orderId,
            @RequestBody(required = false) ParkingExitConfirmRequest request) {
        Long staffUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(parkingExitService.confirmExit(
                orderId,
                request == null ? new ParkingExitConfirmRequest() : request,
                staffUserId
        ));
    }
}
