package com.swp.parking.repository;

import com.swp.parking.model.ParkingFloor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingFloorRepository extends JpaRepository<ParkingFloor, Long> {

    List<ParkingFloor> findByParkingFacilityIdOrderByFloorNumberAscIdAsc(Long parkingId);
}
