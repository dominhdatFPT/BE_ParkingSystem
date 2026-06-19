package com.swp.parking.service;

import com.swp.parking.dto.response.ActiveParkingOrderResponse;
import com.swp.parking.model.ParkingOrder;
import com.swp.parking.repository.ParkingOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        LocalDateTime entryTime = po.getEntryTime();
        Long durationMinutes = entryTime != null
                ? ChronoUnit.MINUTES.between(entryTime, LocalDateTime.now())
                : null;

        return ActiveParkingOrderResponse.builder()
                .orderId(po.getId())
                .status(po.getParkingStatus())
                .licensePlate(po.getVehicle() != null ? po.getVehicle().getLicensePlate() : po.getLicensePlate())
                .vehicleType(po.getVehicle() != null && po.getVehicle().getVehicleType() != null
                        ? po.getVehicle().getVehicleType().getTypeName()
                        : null)
                .facilityName(resolveFacilityName(po))
                .facilityAddress(resolveFacilityAddress(po))
                .floorName(resolveFloorName(po))
                .slotNumber(null)
                .entryTime(entryTime)
                .durationMinutes(durationMinutes)
                .currentFee(po.getCalculatedFee() != null ? po.getCalculatedFee().doubleValue() : null)
                .build();
    }

    private String resolveFacilityName(ParkingOrder po) {
        if (po.getParkingFacility() == null) {
            return null;
        }
        if (po.getParkingFacility().getBuilding() != null
                && po.getParkingFacility().getBuilding().getBuildingName() != null) {
            return po.getParkingFacility().getBuilding().getBuildingName();
        }
        return po.getParkingFacility().getParkingName();
    }

    private String resolveFacilityAddress(ParkingOrder po) {
        if (po.getParkingFacility() == null || po.getParkingFacility().getBuilding() == null) {
            return null;
        }
        return po.getParkingFacility().getBuilding().getAddress();
    }

    private String resolveFloorName(ParkingOrder po) {
        if (po.getParkingFloor() == null) {
            return null;
        }
        if (po.getParkingFloor().getFloorName() != null) {
            return po.getParkingFloor().getFloorName();
        }
        Integer floorNumber = po.getParkingFloor().getFloorNumber();
        return floorNumber != null ? String.valueOf(floorNumber) : null;
    }
}
