package com.swp.parking.admin.pricing.repository;

import com.swp.parking.model.FeePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface AdminFeePackageRepository extends JpaRepository<FeePackage, Long> {

    @Query(value = """
            SELECT fp.fee_package_id     AS "feePackageId",
                   vt.vehicle_type_id    AS "vehicleTypeId",
                   vt.type_name          AS "vehicleTypeName",
                   fp.name               AS name,
                   fp.duration_months    AS "durationMonths",
                   fp.benefits           AS benefits,
                   fp.is_popular         AS "isPopular",
                   fp.is_best_value      AS "isBestValue",
                   fp.is_active          AS "isActive",
                   ph.price_history_id   AS "priceHistoryId",
                   ph.price              AS "currentPrice",
                   ph.original_price     AS "originalPrice",
                   ph.discount_percent   AS "discountPercent",
                   ph.effective_from     AS "effectiveFrom"
              FROM fee_package fp
              JOIN vehicle_types vt ON vt.vehicle_type_id = fp.vehicle_type_id
              JOIN fee_package_price_history ph ON ph.fee_package_id = fp.fee_package_id
                                              AND ph.effective_to IS NULL
             ORDER BY fp.vehicle_type_id, fp.duration_months, fp.fee_package_id
            """, nativeQuery = true)
    List<FeePackageWithCurrentPrice> findAllWithCurrentPrice();

    interface FeePackageWithCurrentPrice {
        Long getFeePackageId();
        Long getVehicleTypeId();
        String getVehicleTypeName();
        String getName();
        Integer getDurationMonths();
        String getBenefits();
        Boolean getIsPopular();
        Boolean getIsBestValue();
        Boolean getIsActive();
        Long getPriceHistoryId();
        BigDecimal getCurrentPrice();
        BigDecimal getOriginalPrice();
        Integer getDiscountPercent();
        LocalDateTime getEffectiveFrom();
    }
}
