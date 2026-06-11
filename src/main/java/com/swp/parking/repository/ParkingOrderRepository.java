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
        JOIN FETCH po.parkingFloor pfl
        WHERE c.user.id = :userId
          AND po.parkingStatus = 'ACTIVE'
        ORDER BY po.entryTime DESC
        """)
    List<ParkingOrder> findAllActiveByUserId(@Param("userId") Long userId);
}
