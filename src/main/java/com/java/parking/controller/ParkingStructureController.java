package com.swp.parking.controller;

import com.swp.parking.dto.response.ParkingFloorResponse;
import com.swp.parking.service.ParkingStructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parking-map")
@RequiredArgsConstructor
public class ParkingStructureController {

    private final ParkingStructureService parkingStructureService;

    @GetMapping("/facility/{parkingId}")
    public ResponseEntity<List<ParkingFloorResponse>> getParkingMap(@PathVariable Long parkingId) {
        return ResponseEntity.ok(parkingStructureService.getParkingStructureByFacility(parkingId));
    }
}
