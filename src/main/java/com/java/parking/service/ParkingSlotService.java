package com.swp.parking.service;

import com.swp.parking.dto.request.ParkingSlotRequest;
import com.swp.parking.dto.response.ParkingSlotResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.ParkingSlot;
import com.swp.parking.model.enums.ParkingSlotStatus;
import com.swp.parking.repository.ParkingSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingSlotService {

    private final ParkingSlotRepository parkingSlotRepository;

    @Transactional(readOnly = true)
    public List<ParkingSlotResponse> getAllParkingSlots() {
        return parkingSlotRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ParkingSlotResponse getParkingSlotById(Long id) {
        ParkingSlot slot = parkingSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking slot not found"));
        return mapToResponse(slot);
    }

    public ParkingSlotResponse createParkingSlot(ParkingSlotRequest request) {
        if (parkingSlotRepository.existsBySlotNumberAndFloor(request.getSlotNumber(), request.getFloor())) {
            throw new AppException(HttpStatus.CONFLICT, "Parking slot already exists on this floor");
        }

        ParkingSlot slot = ParkingSlot.builder()
                .slotNumber(request.getSlotNumber())
                .floor(request.getFloor())
                .status(request.getStatus())
                .build();

        return mapToResponse(parkingSlotRepository.save(slot));
    }

    public ParkingSlotResponse updateParkingSlot(Long id, ParkingSlotRequest request) {
        ParkingSlot slot = parkingSlotRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking slot not found"));

        slot.setSlotNumber(request.getSlotNumber());
        slot.setFloor(request.getFloor());
        slot.setStatus(request.getStatus());

        return mapToResponse(parkingSlotRepository.save(slot));
    }

    public void deleteParkingSlot(Long id) {
        if (!parkingSlotRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Parking slot not found");
        }
        parkingSlotRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ParkingSlotResponse> getAvailableSlots() {
        return parkingSlotRepository.findByStatus(ParkingSlotStatus.AVAILABLE).stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ParkingSlotResponse mapToResponse(ParkingSlot slot) {
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
