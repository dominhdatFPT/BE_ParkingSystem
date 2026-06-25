package com.swp.parking.repository;

import com.swp.parking.model.ParkingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ParkingOrderRepository extends JpaRepository<ParkingOrder, Long> {

    @Query("""
        SELECT COUNT(po)
        FROM ParkingOrder po
        JOIN po.vehicle v
        JOIN v.vehicleType vt
        WHERE po.parkingFacility.id = :parkingId
          AND po.parkingFloor.id = :floorId
          AND vt.id = :vehicleTypeId
          AND po.parkingStatus = 'ACTIVE'
        """)
    long countActiveVehicles(
            @Param("parkingId") Long parkingId,
            @Param("floorId") Long floorId,
            @Param("vehicleTypeId") Long vehicleTypeId
    );
}
