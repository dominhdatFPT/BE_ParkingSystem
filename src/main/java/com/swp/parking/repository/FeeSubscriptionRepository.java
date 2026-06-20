package com.swp.parking.repository;

import com.swp.parking.model.FeeSubscription;
import com.swp.parking.model.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FeeSubscriptionRepository extends JpaRepository<FeeSubscription, Long> {

    @Query("SELECT fs FROM FeeSubscription fs JOIN FETCH fs.vehicle v JOIN FETCH v.customer c WHERE c.user.id = :userId ORDER BY fs.createdAt DESC")
    List<FeeSubscription> findByUserId(@Param("userId") Long userId);

    boolean existsByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);

    Optional<FeeSubscription> findByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);

    List<FeeSubscription> findAllByVehicle_IdAndStatus(Long vehicleId, SubscriptionStatus status);

    @Query("""
            SELECT fs FROM FeeSubscription fs
            JOIN FETCH fs.vehicle v
            JOIN FETCH fs.feePackage
            JOIN FETCH fs.priceHistory
            WHERE v.customer.user.id = :userId
            ORDER BY fs.createdAt DESC
            """)
    List<FeeSubscription> findAllByUserId(@Param("userId") Long userId);
}
