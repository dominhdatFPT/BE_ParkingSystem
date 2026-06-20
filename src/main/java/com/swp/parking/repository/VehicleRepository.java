package com.swp.parking.repository;

import com.swp.parking.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId")
    List<Vehicle> findByUserId(@Param("userId") Long userId);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId AND v.vehicleType.id = :vehicleTypeId")
    List<Vehicle> findByUserIdAndVehicleTypeId(@Param("userId") Long userId, @Param("vehicleTypeId") Long vehicleTypeId);

    boolean existsByIdAndCustomer_User_Id(Long vehicleId, Long userId);
}
