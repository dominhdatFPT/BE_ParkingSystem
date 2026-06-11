package com.swp.parking.service;

import com.swp.parking.dto.response.ActiveParkingOrderResponse;

import java.util.List;

public interface CustomerParkingOrderService {

    List<ActiveParkingOrderResponse> getActiveParkingOrders(Long userId);
}
