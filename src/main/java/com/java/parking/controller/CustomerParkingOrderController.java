package com.swp.parking.controller;

import com.swp.parking.dto.response.ActiveParkingOrderResponse;
import com.swp.parking.dto.response.ApiResponse;
import com.swp.parking.service.CustomerParkingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer/parking-orders")
@RequiredArgsConstructor
public class CustomerParkingOrderController {

    private final CustomerParkingOrderService customerParkingOrderService;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ActiveParkingOrderResponse>>> getActiveParkingOrders() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ActiveParkingOrderResponse> data = customerParkingOrderService.getActiveParkingOrders(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
