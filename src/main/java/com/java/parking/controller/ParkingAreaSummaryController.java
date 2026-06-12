package com.swp.parking.controller;

import com.swp.parking.dto.response.ParkingAreaSummaryResponse;
import com.swp.parking.service.ParkingAreaSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/parking-area-summary")
@RequiredArgsConstructor
public class ParkingAreaSummaryController {

    private final ParkingAreaSummaryService parkingAreaSummaryService;

    @GetMapping
    public ResponseEntity<List<ParkingAreaSummaryResponse>> getAreas(
            @RequestParam(required = false) String buildingCode,
            @RequestParam(required = false) Integer floorNumber
    ) {
        return ResponseEntity.ok(parkingAreaSummaryService.getAreas(buildingCode, floorNumber));
    }
}
