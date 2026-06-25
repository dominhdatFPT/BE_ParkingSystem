package com.swp.parking.repository;

import com.swp.parking.model.Vehicle;
import com.swp.parking.model.VehicleRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId ORDER BY v.createdAt DESC")
    List<Vehicle> findByUserId(@Param("userId") Long userId);

    @Query("SELECT v FROM Vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId AND v.vehicleType.id = :vehicleTypeId ORDER BY v.createdAt DESC")
    List<Vehicle> findByUserIdAndVehicleTypeId(@Param("userId") Long userId, @Param("vehicleTypeId") Long vehicleTypeId);

    @Query("""
            SELECT v FROM Vehicle v
            JOIN FETCH v.customer c
            WHERE c.user.id = :userId
              AND NOT EXISTS (
                  SELECT 1 FROM VehicleRegistration r
                  WHERE r.vehicle = v
                    AND r.isDeleted = true
                    AND r.createdAt = (
                        SELECT MAX(r2.createdAt) FROM VehicleRegistration r2
                        WHERE r2.vehicle = v
                    )
              )
            ORDER BY v.createdAt DESC
            """)
    List<Vehicle> findActiveByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT v FROM Vehicle v
            JOIN FETCH v.customer c
            WHERE c.user.id = :userId
              AND v.vehicleType.id = :vehicleTypeId
              AND NOT EXISTS (
                  SELECT 1 FROM VehicleRegistration r
                  WHERE r.vehicle = v
                    AND r.isDeleted = true
                    AND r.createdAt = (
                        SELECT MAX(r2.createdAt) FROM VehicleRegistration r2
                        WHERE r2.vehicle = v
                    )
              )
            ORDER BY v.createdAt DESC
            """)
    List<Vehicle> findActiveByUserIdAndVehicleTypeId(@Param("userId") Long userId, @Param("vehicleTypeId") Long vehicleTypeId);

    boolean existsByIdAndCustomer_User_Id(Long vehicleId, Long userId);

    Optional<Vehicle> findByLicensePlate(String licensePlate);
}
