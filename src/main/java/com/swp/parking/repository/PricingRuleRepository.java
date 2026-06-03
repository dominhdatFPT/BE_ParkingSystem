package com.swp.parking.repository;

import com.swp.parking.entity.PricingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository truy vấn quy tắc giá (pricing_rules) theo bãi và loại xe.
 */
public interface PricingRuleRepository extends JpaRepository<PricingRule, Long> {

    /**
     * Lấy các rule đang active, ưu tiên cao trước (priority DESC).
     */
    List<PricingRule> findByParkingIdAndVehicleTypeIdAndIsActiveTrueOrderByPriorityDesc(
            Long parkingId, Long vehicleTypeId);
}
