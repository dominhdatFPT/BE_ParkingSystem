package com.swp.parking.service;

import com.swp.parking.dto.response.FeePackageResponse;
import com.swp.parking.model.FeePackage;
import com.swp.parking.model.FeePackagePriceHistory;
import com.swp.parking.repository.FeePackagePriceHistoryRepository;
import com.swp.parking.repository.FeePackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeePackageService {

    private final FeePackageRepository feePackageRepository;
    private final FeePackagePriceHistoryRepository priceHistoryRepository;

    public List<FeePackageResponse> getFeePackages(Long vehicleTypeId) {
        log.info("Fetching fee packages, vehicleTypeId: {}", vehicleTypeId);

        List<FeePackage> packages;
        if (vehicleTypeId != null) {
            packages = feePackageRepository.findByVehicleType_IdAndIsActiveTrue(vehicleTypeId);
        } else {
            packages = feePackageRepository.findByIsActiveTrue();
        }

        LocalDateTime now = LocalDateTime.now();

        return packages.stream()
                .map(fp -> toResponse(fp, now))
                .collect(Collectors.toList());
    }

    private FeePackageResponse toResponse(FeePackage fp, LocalDateTime now) {
        FeePackagePriceHistory priceHistory = priceHistoryRepository
                .findFirstByFeePackage_IdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(fp.getId(), now)
                .orElse(null);

        BigDecimal currentPrice = priceHistory != null ? priceHistory.getPrice() : null;
        BigDecimal originalPrice = priceHistory != null ? priceHistory.getOriginalPrice() : null;
        Integer discountPercent = priceHistory != null ? priceHistory.getDiscountPercent() : null;

        return FeePackageResponse.builder()
                .id(fp.getId())
                .vehicleTypeId(fp.getVehicleType().getId())
                .vehicleTypeName(fp.getVehicleType().getTypeName())
                .name(fp.getName())
                .durationMonths(fp.getDurationMonths())
                .benefits(fp.getBenefits())
                .isPopular(fp.getIsPopular())
                .isBestValue(fp.getIsBestValue())
                .currentPrice(currentPrice)
                .price(currentPrice)
                .originalPrice(originalPrice)
                .discountPercent(discountPercent)
                .build();
    }
}
