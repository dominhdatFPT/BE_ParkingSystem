package com.swp.parking.admin.pricing.service;

import com.swp.parking.admin.pricing.dto.request.UpdatePriceRequest;
import com.swp.parking.admin.pricing.dto.request.UpdateVisitorRateRequest;
import com.swp.parking.dto.response.FeePackageResponse;
import com.swp.parking.admin.pricing.dto.response.VisitorFeeRateResponse;
import com.swp.parking.admin.pricing.model.VisitorFeeRate;
import com.swp.parking.admin.pricing.repository.AdminFeePackagePriceHistoryRepository;
import com.swp.parking.admin.pricing.repository.AdminFeePackageRepository;
import com.swp.parking.admin.pricing.repository.VisitorFeeRateRepository;
import com.swp.parking.exception.NotFoundException;
import com.swp.parking.model.FeePackage;
import com.swp.parking.model.FeePackagePriceHistory;
import com.swp.parking.model.VehicleType;
import com.swp.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final AdminFeePackageRepository adminFeePackageRepository;
    private final AdminFeePackagePriceHistoryRepository adminFeePackagePriceHistoryRepository;
    private final VisitorFeeRateRepository visitorFeeRateRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    @Transactional(readOnly = true)
    public List<FeePackageResponse> getAllFeePackages() {
        log.info("Fetching all fee packages with current price");
        return adminFeePackageRepository.findAllWithCurrentPrice()
                .stream()
                .map(this::toFeePackageResponse)
                .toList();
    }

    @Transactional
    public FeePackageResponse updateFeePackagePrice(Long feePackageId, UpdatePriceRequest request) {
        log.info("Updating price for fee package id: {}", feePackageId);

        FeePackage feePackage = adminFeePackageRepository.findById(feePackageId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy gói phí với ID: " + feePackageId));

        FeePackagePriceHistory activePrice = adminFeePackagePriceHistoryRepository
                .findActiveByFeePackage(feePackage)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy giá hiện tại của gói phí ID: " + feePackageId));

        LocalDateTime now = LocalDateTime.now();

        activePrice.setEffectiveTo(now);
        adminFeePackagePriceHistoryRepository.save(activePrice);

        FeePackagePriceHistory newPrice = FeePackagePriceHistory.builder()
                .feePackage(feePackage)
                .originalPrice(request.getOriginalPrice())
                .price(request.getPrice())
                .discountPercent(request.getDiscountPercent())
                .effectiveFrom(now)
                .effectiveTo(null)
                .build();
        adminFeePackagePriceHistoryRepository.save(newPrice);

        feePackage.setUpdatedAt(now);
        adminFeePackageRepository.save(feePackage);

        log.info("Updated price for fee package id: {} (new price history id: {})",
                feePackageId, newPrice.getId());

        return buildFeePackageResponse(feePackage, newPrice);
    }

    @Transactional
    public void toggleFeePackage(Long feePackageId) {
        log.info("Toggling fee package id: {}", feePackageId);

        FeePackage feePackage = adminFeePackageRepository.findById(feePackageId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy gói phí với ID: " + feePackageId));

        Boolean current = feePackage.getIsActive();
        feePackage.setIsActive(current == null || !current);
        adminFeePackageRepository.save(feePackage);

        log.info("Toggled fee package id: {} to isActive={}", feePackageId, feePackage.getIsActive());
    }

    @Transactional(readOnly = true)
    public List<VisitorFeeRateResponse> getAllVisitorRates() {
        log.info("Fetching all active visitor fee rates");
        return visitorFeeRateRepository.findAllActiveWithVehicleType()
                .stream()
                .map(this::toVisitorFeeRateResponse)
                .toList();
    }

    @Transactional
    public VisitorFeeRateResponse updateVisitorRate(Long vehicleTypeId, UpdateVisitorRateRequest request) {
        log.info("Updating visitor fee rate for vehicle type id: {}", vehicleTypeId);

        VehicleType vehicleType = vehicleTypeRepository.findById(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy loại xe với ID: " + vehicleTypeId));

        VisitorFeeRate activeRate = visitorFeeRateRepository.findActiveByVehicleTypeId(vehicleTypeId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy giá vãng lai đang hoạt động cho loại xe ID: " + vehicleTypeId));

        LocalDateTime now = LocalDateTime.now();

        activeRate.setIsActive(false);
        activeRate.setEffectiveTo(now);
        visitorFeeRateRepository.save(activeRate);

        VisitorFeeRate newRate = VisitorFeeRate.builder()
                .vehicleType(vehicleType)
                .parkingFacility(activeRate.getParkingFacility())
                .firstBlockMinutes(request.getFirstBlockMinutes())
                .firstBlockFee(request.getFirstBlockFee())
                .nextBlockMinutes(request.getNextBlockMinutes())
                .nextBlockFee(request.getNextBlockFee())
                .dailyCap(request.getDailyCap())
                .overnightFee(request.getOvernightFee())
                .effectiveFrom(now)
                .effectiveTo(null)
                .isActive(true)
                .build();
        VisitorFeeRate saved = visitorFeeRateRepository.save(newRate);

        log.info("Updated visitor fee rate for vehicle type id: {} (new rate id: {})",
                vehicleTypeId, saved.getId());

        return VisitorFeeRateResponse.builder()
                .feeRateId(saved.getId())
                .vehicleTypeId(vehicleType.getId())
                .vehicleTypeName(vehicleType.getTypeName())
                .firstBlockMinutes(saved.getFirstBlockMinutes())
                .firstBlockFee(saved.getFirstBlockFee())
                .nextBlockMinutes(saved.getNextBlockMinutes())
                .nextBlockFee(saved.getNextBlockFee())
                .dailyCap(saved.getDailyCap())
                .overnightFee(saved.getOvernightFee())
                .effectiveFrom(saved.getEffectiveFrom())
                .isActive(saved.getIsActive())
                .build();
    }

    private FeePackageResponse toFeePackageResponse(AdminFeePackageRepository.FeePackageWithCurrentPrice row) {
        return FeePackageResponse.builder()
                .id(row.getFeePackageId())
                .vehicleTypeId(row.getVehicleTypeId())
                .vehicleTypeName(row.getVehicleTypeName())
                .name(row.getName())
                .durationMonths(row.getDurationMonths())
                .benefits(splitBenefits(row.getBenefits()))
                .isPopular(row.getIsPopular())
                .isBestValue(row.getIsBestValue())
                .isActive(row.getIsActive())
                .currentPrice(row.getCurrentPrice())
                .originalPrice(row.getOriginalPrice())
                .discountPercent(row.getDiscountPercent())
                .priceHistoryId(row.getPriceHistoryId())
                .effectiveFrom(row.getEffectiveFrom())
                .build();
    }

    private FeePackageResponse buildFeePackageResponse(FeePackage feePackage, FeePackagePriceHistory priceHistory) {
        VehicleType vehicleType = feePackage.getVehicleType();
        return FeePackageResponse.builder()
                .id(feePackage.getId())
                .vehicleTypeId(vehicleType != null ? vehicleType.getId() : null)
                .vehicleTypeName(vehicleType != null ? vehicleType.getTypeName() : null)
                .name(feePackage.getName())
                .durationMonths(feePackage.getDurationMonths())
                .benefits(splitBenefits(feePackage.getBenefits()))
                .isPopular(feePackage.getIsPopular())
                .isBestValue(feePackage.getIsBestValue())
                .isActive(feePackage.getIsActive())
                .currentPrice(priceHistory.getPrice())
                .originalPrice(priceHistory.getOriginalPrice())
                .discountPercent(priceHistory.getDiscountPercent())
                .priceHistoryId(priceHistory.getId())
                .effectiveFrom(priceHistory.getEffectiveFrom())
                .build();
    }

    private VisitorFeeRateResponse toVisitorFeeRateResponse(
            VisitorFeeRateRepository.VisitorFeeRateWithVehicleType row) {
        return VisitorFeeRateResponse.builder()
                .feeRateId(row.getFeeRateId())
                .vehicleTypeId(row.getVehicleTypeId())
                .vehicleTypeName(row.getVehicleTypeName())
                .firstBlockMinutes(row.getFirstBlockMinutes())
                .firstBlockFee(row.getFirstBlockFee())
                .nextBlockMinutes(row.getNextBlockMinutes())
                .nextBlockFee(row.getNextBlockFee())
                .dailyCap(row.getDailyCap())
                .overnightFee(row.getOvernightFee())
                .effectiveFrom(row.getEffectiveFrom())
                .isActive(row.getIsActive())
                .build();
    }

    private List<String> splitBenefits(String benefits) {
        if (benefits == null || benefits.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(benefits.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
