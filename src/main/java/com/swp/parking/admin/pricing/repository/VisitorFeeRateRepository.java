package com.swp.parking.admin.pricing.repository;

import com.swp.parking.admin.pricing.model.VisitorFeeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface VisitorFeeRateRepository extends JpaRepository<VisitorFeeRate, Long> {

    @Query("""
            SELECT vfr FROM VisitorFeeRate vfr
            WHERE vfr.vehicleType.id = :vehicleTypeId
              AND vfr.isActive = true
              AND vfr.effectiveTo IS NULL
            """)
    Optional<VisitorFeeRate> findActiveByVehicleTypeId(@Param("vehicleTypeId") Long vehicleTypeId);

    @Query(value = """
            SELECT vfr.fee_rate_id       AS "feeRateId",
                   vt.vehicle_type_id    AS "vehicleTypeId",
                   vt.type_name          AS "vehicleTypeName",
                   vfr.first_block_minutes AS "firstBlockMinutes",
                   vfr.first_block_fee   AS "firstBlockFee",
                   vfr.next_block_minutes  AS "nextBlockMinutes",
                   vfr.next_block_fee    AS "nextBlockFee",
                   vfr.daily_cap         AS "dailyCap",
                   vfr.overnight_fee     AS "overnightFee",
                   vfr.effective_from    AS "effectiveFrom",
                   vfr.is_active         AS "isActive"
              FROM visitor_fee_rates vfr
              JOIN vehicle_types vt ON vt.vehicle_type_id = vfr.vehicle_type_id
             WHERE vfr.is_active = true
               AND vfr.effective_to IS NULL
             ORDER BY vt.vehicle_type_id
            """, nativeQuery = true)
    List<VisitorFeeRateWithVehicleType> findAllActiveWithVehicleType();

    interface VisitorFeeRateWithVehicleType {
        Long getFeeRateId();
        Long getVehicleTypeId();
        String getVehicleTypeName();
        Integer getFirstBlockMinutes();
        BigDecimal getFirstBlockFee();
        Integer getNextBlockMinutes();
        BigDecimal getNextBlockFee();
        BigDecimal getDailyCap();
        BigDecimal getOvernightFee();
        java.time.LocalDateTime getEffectiveFrom();
        Boolean getIsActive();
    }
}
