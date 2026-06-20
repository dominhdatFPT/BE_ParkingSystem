package com.swp.parking.repository;

import com.swp.parking.model.MomoOrder;
import com.swp.parking.model.enums.MomoOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MomoOrderRepository extends JpaRepository<MomoOrder, String> {

    Optional<MomoOrder> findFirstBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    List<MomoOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MomoOrder> findByPaymentStatusOrderByCreatedAtDesc(MomoOrderStatus status);
}
