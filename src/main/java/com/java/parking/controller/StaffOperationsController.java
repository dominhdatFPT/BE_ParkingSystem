package com.swp.parking.controller;

import com.swp.parking.dto.response.OperationsDashboardResponse;
import com.swp.parking.dto.response.ParkingOperationsResponse;
import com.swp.parking.service.OperationsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
public class StaffOperationsController {

    private final OperationsDashboardService operationsDashboardService;

    @GetMapping("/operations-dashboard")
    public ResponseEntity<OperationsDashboardResponse> getOperationsDashboard() {
        return ResponseEntity.ok(operationsDashboardService.getDashboard());
    }

    @GetMapping("/parking-operations")
    public ResponseEntity<ParkingOperationsResponse> getParkingOperations() {
        return ResponseEntity.ok(operationsDashboardService.getParkingOperations());
    }
}
