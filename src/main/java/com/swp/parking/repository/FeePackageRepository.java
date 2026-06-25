package com.swp.parking.repository;

import com.swp.parking.model.FeePackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface FeePackageRepository extends JpaRepository<FeePackage, Long> {

    List<FeePackage> findByIsActiveTrue();

    @Query("SELECT fp FROM FeePackage fp WHERE fp.vehicleType.id = :vehicleTypeId AND fp.isActive = true")
    List<FeePackage> findByVehicleType_IdAndIsActiveTrue(@Param("vehicleTypeId") Long vehicleTypeId);

    @Query(value = """
            SELECT fp.fee_package_id AS id,
                   vt.vehicle_type_id AS "vehicleTypeId",
                   vt.type_name AS "vehicleTypeName",
                   fp.name AS name,
                   fp.duration_months AS "durationMonths",
                   fp.benefits AS benefits,
                   fp.is_popular AS "isPopular",
                   fp.is_best_value AS "isBestValue",
                   current_price.price AS "currentPrice",
                   current_price.original_price AS "originalPrice",
                   current_price.discount_percent AS "discountPercent"
              FROM fee_package fp
              JOIN vehicle_types vt ON vt.vehicle_type_id = fp.vehicle_type_id
              LEFT JOIN LATERAL (
                    SELECT ph.price,
                           ph.original_price,
                           ph.discount_percent
                      FROM fee_package_price_history ph
                     WHERE ph.fee_package_id = fp.fee_package_id
                       AND ph.effective_from <= :at
                     ORDER BY ph.effective_from DESC
                     LIMIT 1
              ) current_price ON true
             WHERE fp.is_active = true
               AND (:vehicleTypeId IS NULL OR fp.vehicle_type_id = :vehicleTypeId)
             ORDER BY fp.duration_months, fp.fee_package_id
            """, nativeQuery = true)
    List<FeePackageWithCurrentPrice> findActiveWithCurrentPrice(
            @Param("vehicleTypeId") Long vehicleTypeId,
            @Param("at") LocalDateTime at);

    interface FeePackageWithCurrentPrice {
        Long getId();
        Long getVehicleTypeId();
        String getVehicleTypeName();
        String getName();
        Integer getDurationMonths();
        String getBenefits();
        Boolean getIsPopular();
        Boolean getIsBestValue();
        BigDecimal getCurrentPrice();
        BigDecimal getOriginalPrice();
        Integer getDiscountPercent();
    }
}
