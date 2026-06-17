package com.swp.parking.repository;

import com.swp.parking.model.FeePackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeePackageRepository extends JpaRepository<FeePackage, Long> {

    List<FeePackage> findByIsActiveTrue();

    List<FeePackage> findByVehicleType_IdAndIsActiveTrue(Long vehicleTypeId);
}
