package com.swp.parking.controller;

import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.dto.response.ParkingLocationResponse;
import com.swp.parking.dto.response.VehicleInfoResponse;
import com.swp.parking.service.ParkingLocationService;
import com.swp.parking.service.VehicleInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API thông tin đỗ xe cho khách hàng đã đăng nhập.
 */
@RestController
@RequestMapping("/api/parking/me")
@RequiredArgsConstructor
@Slf4j
public class ParkingInfoController {

    private final VehicleInfoService vehicleInfoService;
    private final ParkingLocationService parkingLocationService;

    /**
     * GET /api/parking/me/vehicles – danh sách xe đang gửi của user hiện tại.
     */
    @GetMapping("/vehicles")
    public ResponseEntity<ApiResponse<List<VehicleInfoResponse>>> getMyActiveVehicles() {
        // Principal là userId đã được security layer gắn sau khi đăng nhập
        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("Yêu cầu danh sách xe đang gửi, userId={}", userId);

        List<VehicleInfoResponse> vehicles = vehicleInfoService.getActiveVehicles(userId);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    /**
     * GET /api/parking/me/locations – vị trí đỗ (tầng/bãi) và thời lượng gửi cho từng xe active.
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<ParkingLocationResponse>>> getMyParkingLocations() {
        // Principal là userId đã được security layer gắn sau khi đăng nhập
        Long userId = (Long) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        log.info("Yêu cầu vị trí đỗ xe, userId={}", userId);

        List<ParkingLocationResponse> locations = parkingLocationService.getParkingLocations(userId);
        return ResponseEntity.ok(ApiResponse.success(locations));
    }
}
