package com.swp.parking.repository;

import com.swp.parking.model.ParkingFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingFacilityRepository extends JpaRepository<ParkingFacility, Long> {
}
