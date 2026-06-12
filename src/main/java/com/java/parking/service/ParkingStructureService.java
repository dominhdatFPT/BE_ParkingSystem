package com.swp.parking.service;

import com.swp.parking.dto.response.ParkingFloorResponse;
import com.swp.parking.dto.response.ParkingSlotResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.ParkingFacility;
import com.swp.parking.model.ParkingFloor;
import com.swp.parking.model.ParkingSlot;
import com.swp.parking.repository.ParkingFacilityRepository;
import com.swp.parking.repository.ParkingFloorRepository;
import com.swp.parking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingStructureService {

    private final ParkingFacilityRepository parkingFacilityRepository;
    private final ParkingFloorRepository parkingFloorRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    public List<ParkingFloorResponse> getParkingStructureByFacility(Long parkingId) {
        // Verify facility exists
        if (!parkingFacilityRepository.existsById(parkingId)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Parking facility not found");
        }

        // Get all floors for this facility
        List<ParkingFloor> floors = parkingFloorRepository.findByParkingFacilityIdOrderByFloorNumberAscIdAsc(parkingId);

        return floors.stream()
                .map(floor -> mapFloorToResponse(floor, parkingId))
                .collect(Collectors.toList());
    }

    private ParkingFloorResponse mapFloorToResponse(ParkingFloor floor, Long parkingId) {
        // Get all slots for this floor (assuming slots have a floor column that matches floor.floorNumber)
        List<ParkingSlot> slots = parkingSlotRepository.findByFloor(floor.getFloorNumber());

        List<ParkingSlotResponse> slotResponses = slots.stream()
                .map(this::mapSlotToResponse)
                .collect(Collectors.toList());

        return ParkingFloorResponse.builder()
                .id(floor.getId())
                .floorName(floor.getFloorName())
                .floorNumber(floor.getFloorNumber())
                .parkingId(parkingId)
                .slots(slotResponses)
                .build();
    }

    private ParkingSlotResponse mapSlotToResponse(ParkingSlot slot) {
        return ParkingSlotResponse.builder()
                .id(slot.getId())
                .slotNumber(slot.getSlotNumber())
                .floor(slot.getFloor())
                .status(slot.getStatus())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .build();
    }
}

