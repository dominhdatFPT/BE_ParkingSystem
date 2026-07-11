package com.swp.parking.admin.pricing.repository;

import com.swp.parking.model.FeePackage;
import com.swp.parking.model.FeePackagePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminFeePackagePriceHistoryRepository extends JpaRepository<FeePackagePriceHistory, Long> {

    @Query("""
            SELECT ph FROM FeePackagePriceHistory ph
            WHERE ph.feePackage = :feePackage
              AND ph.effectiveTo IS NULL
            """)
    Optional<FeePackagePriceHistory> findActiveByFeePackage(@Param("feePackage") FeePackage feePackage);
}
