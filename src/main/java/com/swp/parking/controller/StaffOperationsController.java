package com.swp.parking.controller;

import com.swp.parking.dto.response.OperationsDashboardResponse;
import com.swp.parking.dto.response.ParkingOperationsResponse;
import com.swp.parking.service.OperationsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffOperationsController {

    private final OperationsDashboardService operationsDashboardService;

    @GetMapping("/operations-dashboard")
    public ResponseEntity<OperationsDashboardResponse> getOperationsDashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(operationsDashboardService.getDashboard(date));
    }

    @GetMapping("/parking-operations")
    public ResponseEntity<ParkingOperationsResponse> getParkingOperations() {
        return ResponseEntity.ok(operationsDashboardService.getParkingOperations());
    }

    @GetMapping("/parking-sessions")
    public ResponseEntity<List<OperationsDashboardResponse.VehicleActivity>> getParkingSessions() {
        return ResponseEntity.ok(operationsDashboardService.getParkingSessions());
    }
}
