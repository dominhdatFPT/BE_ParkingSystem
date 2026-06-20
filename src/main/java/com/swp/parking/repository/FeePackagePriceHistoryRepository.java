package com.swp.parking.repository;

import com.swp.parking.model.FeePackagePriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FeePackagePriceHistoryRepository extends JpaRepository<FeePackagePriceHistory, Long> {

    Optional<FeePackagePriceHistory> findFirstByFeePackage_IdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(
            Long feePackageId, LocalDateTime dateTime);

    Optional<FeePackagePriceHistory> findFirstByFeePackage_IdAndEffectiveToIsNullOrderByEffectiveFromDesc(
            Long feePackageId);
}
