package com.swp.parking.repository;

import com.swp.parking.model.ParkingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingOrderRepository extends JpaRepository<ParkingOrder, Long> {

    @Query("""
        SELECT po
        FROM ParkingOrder po
        JOIN FETCH po.vehicle v
        JOIN FETCH v.customer c
        JOIN FETCH v.vehicleType vt
        JOIN FETCH po.parkingFacility pf
        LEFT JOIN FETCH pf.building b
        JOIN FETCH po.parkingFloor pfl
        WHERE c.user.id = :userId
          AND po.parkingStatus = 'ACTIVE'
        ORDER BY po.entryTime DESC
        """)
    List<ParkingOrder> findAllActiveByUserId(@Param("userId") Long userId);

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
