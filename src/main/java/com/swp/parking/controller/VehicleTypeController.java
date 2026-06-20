package com.swp.parking.controller;

import com.swp.parking.dto.response.VehicleTypeResponse;
import com.swp.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-types")
@RequiredArgsConstructor
public class VehicleTypeController {

    private final VehicleTypeRepository vehicleTypeRepository;

    @GetMapping
    public ResponseEntity<List<VehicleTypeResponse>> getVehicleTypes() {
        return ResponseEntity.ok(vehicleTypeRepository.findAll().stream()
                .map(type -> VehicleTypeResponse.builder()
                        .id(type.getId())
                        .typeName(type.getTypeName())
                        .typeCode(type.getTypeCode())
                        .build())
                .toList());
    }
}
