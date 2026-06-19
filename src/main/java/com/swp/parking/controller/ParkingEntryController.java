package com.swp.parking.controller;

import com.swp.parking.dto.request.ParkingEntryCheckRequest;
import com.swp.parking.dto.request.ParkingEntryConfirmRequest;
import com.swp.parking.dto.response.ParkingEntryResponse;
import com.swp.parking.service.ParkingEntryService;
import com.swp.parking.service.SecurityRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parking-entry")
@RequiredArgsConstructor
public class ParkingEntryController {

    private final ParkingEntryService parkingEntryService;
    private final SecurityRoleService securityRoleService;

    @PostMapping("/check")
    public ResponseEntity<ParkingEntryResponse> checkVehicle(@RequestBody ParkingEntryCheckRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(parkingEntryService.checkVehicle(request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ParkingEntryResponse> confirmEntry(@RequestBody ParkingEntryConfirmRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        Long staffUserId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(parkingEntryService.confirmEntry(request, staffUserId));
    }
}
