package com.swp.parking.repository;

import com.swp.parking.model.ParkingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {

    List<ParkingZone> findByParkingFacilityIdOrderByIdAsc(Long parkingId);

    List<ParkingZone> findByParkingFacilityIdAndParkingFloorIdOrderByIdAsc(Long parkingId, Long floorId);

    Optional<ParkingZone> findByParkingFacilityIdAndParkingFloorIdAndVehicleTypeId(
            Long parkingId,
            Long floorId,
            Long vehicleTypeId
    );
}
