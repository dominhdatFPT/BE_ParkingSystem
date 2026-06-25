package com.swp.parking.service;

import com.swp.parking.dto.response.FeePackageResponse;
import com.swp.parking.repository.FeePackageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeePackageService {

    private final FeePackageRepository feePackageRepository;

    public List<FeePackageResponse> getFeePackages(Long vehicleTypeId) {
        log.info("Fetching fee packages, vehicleTypeId: {}", vehicleTypeId);

        return feePackageRepository.findActiveWithCurrentPrice(vehicleTypeId, LocalDateTime.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private FeePackageResponse toResponse(FeePackageRepository.FeePackageWithCurrentPrice fp) {
        return FeePackageResponse.builder()
                .id(fp.getId())
                .vehicleTypeId(fp.getVehicleTypeId())
                .vehicleTypeName(fp.getVehicleTypeName())
                .name(fp.getName())
                .durationMonths(fp.getDurationMonths())
                .benefits(fp.getBenefits())
                .isPopular(fp.getIsPopular())
                .isBestValue(fp.getIsBestValue())
                .currentPrice(fp.getCurrentPrice())
                .price(fp.getCurrentPrice())
                .originalPrice(fp.getOriginalPrice())
                .discountPercent(fp.getDiscountPercent())
                .build();
    }
}
