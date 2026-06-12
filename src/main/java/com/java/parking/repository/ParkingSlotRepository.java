package com.swp.parking.repository;

import com.swp.parking.model.ParkingSlot;
import com.swp.parking.model.enums.ParkingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSlotRepository extends JpaRepository<ParkingSlot, Long> {

    List<ParkingSlot> findByStatus(ParkingSlotStatus status);

    Optional<ParkingSlot> findBySlotNumber(String slotNumber);

    boolean existsBySlotNumberAndFloor(String slotNumber, Integer floor);

    List<ParkingSlot> findByFloor(Integer floor);
}
