package com.swp.parking.service;

import com.swp.parking.dto.response.ActiveParkingOrderResponse;
import com.swp.parking.model.ParkingOrder;
import com.swp.parking.repository.ParkingOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerParkingOrderServiceImpl implements CustomerParkingOrderService {

    private final ParkingOrderRepository parkingOrderRepository;

    @Override
    public List<ActiveParkingOrderResponse> getActiveParkingOrders(Long userId) {
        return parkingOrderRepository.findAllActiveByUserId(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ActiveParkingOrderResponse mapToResponse(ParkingOrder po) {
        return ActiveParkingOrderResponse.builder()
                .orderId(po.getId())
                .licensePlate(po.getVehicle() != null ? po.getVehicle().getLicensePlate() : po.getLicensePlate())
                .vehicleTypeName(po.getVehicle() != null && po.getVehicle().getVehicleType() != null
                        ? po.getVehicle().getVehicleType().getTypeName()
                        : null)
                .parkingName(po.getParkingFacility() != null ? po.getParkingFacility().getParkingName() : null)
                .floorName(po.getParkingFloor() != null ? po.getParkingFloor().getFloorName() : null)
                .entryTime(po.getEntryTime())
                .calculatedFee(po.getCalculatedFee())
                .parkingStatus(po.getParkingStatus())
                .build();
    }
}
