package com.swp.parking.repository;

import com.swp.parking.model.VNPayOrder;
import com.swp.parking.model.enums.VNPayOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VNPayOrderRepository extends JpaRepository<VNPayOrder, String> {

    Optional<VNPayOrder> findFirstBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);

    List<VNPayOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<VNPayOrder> findByStatusOrderByCreatedAtDesc(VNPayOrderStatus status);
}
