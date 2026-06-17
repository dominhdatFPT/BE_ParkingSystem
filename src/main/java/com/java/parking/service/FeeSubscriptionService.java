package com.swp.parking.service;

import com.swp.parking.dto.request.CreateSubscriptionRequest;
import com.swp.parking.dto.response.CreateSubscriptionResponse;
import com.swp.parking.dto.response.MyVehicleResponse;
import com.swp.parking.exception.FeePackageNotFoundException;
import com.swp.parking.exception.InvalidSubscriptionStatusException;
import com.swp.parking.exception.SubscriptionNotFoundException;
import com.swp.parking.exception.VehicleAlreadyHasActiveSubscriptionException;
import com.swp.parking.exception.VehicleNotOwnedByUserException;
import com.swp.parking.model.*;
import com.swp.parking.model.enums.SubscriptionStatus;
import com.swp.parking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FeeSubscriptionService {

    private final VehicleRepository vehicleRepository;
    private final FeePackageRepository feePackageRepository;
    private final FeePackagePriceHistoryRepository priceHistoryRepository;
    private final FeeSubscriptionRepository feeSubscriptionRepository;
    private final VehicleTypeRepository vehicleTypeRepository;

    @Transactional(readOnly = true)
    public List<MyVehicleResponse> getMyVehicles(Long userId, Long vehicleTypeId) {
        log.info("Fetching vehicles for userId: {}, vehicleTypeId: {}", userId, vehicleTypeId);

        List<Vehicle> vehicles;
        if (vehicleTypeId != null) {
            vehicles = vehicleRepository.findByUserIdAndVehicleTypeId(userId, vehicleTypeId);
        } else {
            vehicles = vehicleRepository.findByUserId(userId);
        }

        return vehicles.stream()
                .map(v -> MyVehicleResponse.builder()
                        .vehicleId(v.getId())
                        .licensePlate(v.getLicensePlate())
                        .brand(v.getBrand())
                        .color(v.getColor())
                        .vehicleTypeId(v.getVehicleType().getId())
                        .vehicleTypeName(v.getVehicleType().getTypeName())
                        .build())
                .collect(Collectors.toList());
    }

    public CreateSubscriptionResponse createSubscription(Long userId, CreateSubscriptionRequest request) {
        log.info("Creating subscription for userId: {}, vehicleId: {}, feePackageId: {}",
                userId, request.getVehicleId(), request.getFeePackageId());

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new SubscriptionNotFoundException(request.getVehicleId()));

        if (!vehicle.getCustomer().getUser().getId().equals(userId)) {
            throw new VehicleNotOwnedByUserException();
        }

        feeSubscriptionRepository.findByVehicle_IdAndStatus(request.getVehicleId(), SubscriptionStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new VehicleAlreadyHasActiveSubscriptionException(existing.getEndDate());
                });

        FeePackage feePackage = feePackageRepository.findById(request.getFeePackageId())
                .orElseThrow(() -> new FeePackageNotFoundException(request.getFeePackageId()));

        LocalDateTime now = LocalDateTime.now();
        FeePackagePriceHistory priceHistory = priceHistoryRepository
                .findFirstByFeePackage_IdAndEffectiveFromLessThanEqualOrderByEffectiveFromDesc(feePackage.getId(), now)
                .orElseThrow(() -> new FeePackageNotFoundException(request.getFeePackageId()));

        FeeSubscription subscription = FeeSubscription.builder()
                .vehicle(vehicle)
                .feePackage(feePackage)
                .priceHistory(priceHistory)
                .amountToPay(priceHistory.getPrice())
                .status(SubscriptionStatus.PENDING_PAYMENT)
                .build();

        subscription = feeSubscriptionRepository.save(subscription);

        return CreateSubscriptionResponse.builder()
                .subscriptionId(subscription.getId())
                .status(subscription.getStatus().name())
                .amountToPay(subscription.getAmountToPay())
                .feePackageName(feePackage.getName())
                .vehicleLicensePlate(vehicle.getLicensePlate())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    public void cancelSubscription(Long userId, Long subscriptionId) {
        log.info("Cancelling subscription id: {} for userId: {}", subscriptionId, userId);

        FeeSubscription subscription = feeSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));

        if (!subscription.getVehicle().getCustomer().getUser().getId().equals(userId)) {
            throw new VehicleNotOwnedByUserException();
        }

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new InvalidSubscriptionStatusException("Chỉ có thể hủy gói đang hoạt động");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        feeSubscriptionRepository.save(subscription);
    }

    public void confirmPaymentSuccess(Long subscriptionId) {
        // TODO: Gọi callback từ cổng thanh toán để xác nhận thanh toán thành công
        // - Tìm FeeSubscription theo subscriptionId
        // - Kiểm tra status == PENDING_PAYMENT
        // - Set status = ACTIVE
        // - Set startDate = now
        // - Set endDate = startDate + feePackage.durationMonths
        // - Lưu lại
        log.info("confirmPaymentSuccess stub called for subscriptionId: {}", subscriptionId);
    }

    public void cancelPendingPayment(Long subscriptionId) {
        // TODO: Gọi khi thanh toán thất bại hoặc user hủy thanh toán
        // - Tìm FeeSubscription theo subscriptionId
        // - Kiểm tra status == PENDING_PAYMENT
        // - Set status = CANCELLED
        // - Lưu lại
        log.info("cancelPendingPayment stub called for subscriptionId: {}", subscriptionId);
    }
}
