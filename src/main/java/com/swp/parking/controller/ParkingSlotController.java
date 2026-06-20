package com.swp.parking.controller;

import com.swp.parking.dto.request.ParkingSlotRequest;
import com.swp.parking.dto.response.ParkingSlotResponse;
import com.swp.parking.service.ParkingSlotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parking-slots")
@RequiredArgsConstructor
public class ParkingSlotController {

    private final ParkingSlotService parkingSlotService;

    @GetMapping
    public ResponseEntity<List<ParkingSlotResponse>> getAllParkingSlots() {
        return ResponseEntity.ok(parkingSlotService.getAllParkingSlots());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ParkingSlotResponse> getParkingSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(parkingSlotService.getParkingSlotById(id));
    }

    @PostMapping
    public ResponseEntity<ParkingSlotResponse> createParkingSlot(
            @Valid @RequestBody ParkingSlotRequest request) {
        ParkingSlotResponse response = parkingSlotService.createParkingSlot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ParkingSlotResponse> updateParkingSlot(
            @PathVariable Long id,
            @Valid @RequestBody ParkingSlotRequest request) {
        return ResponseEntity.ok(parkingSlotService.updateParkingSlot(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteParkingSlot(@PathVariable Long id) {
        parkingSlotService.deleteParkingSlot(id);
        return ResponseEntity.noContent().build();
    }
}
