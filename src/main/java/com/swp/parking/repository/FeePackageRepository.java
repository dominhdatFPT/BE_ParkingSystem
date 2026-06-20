package com.swp.parking.repository;

import com.swp.parking.model.FeePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeePackageRepository extends JpaRepository<FeePackage, Long> {

    List<FeePackage> findByIsActiveTrue();

    @Query("SELECT fp FROM FeePackage fp WHERE fp.vehicleType.id = :vehicleTypeId AND fp.isActive = true")
    List<FeePackage> findByVehicleType_IdAndIsActiveTrue(@Param("vehicleTypeId") Long vehicleTypeId);
}
