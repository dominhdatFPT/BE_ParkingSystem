package com.swp.parking.repository;

import com.swp.parking.model.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehicleTypeRepository extends JpaRepository<VehicleType, Long> {
    Optional<VehicleType> findFirstByTypeCodeIgnoreCase(String typeCode);
}
