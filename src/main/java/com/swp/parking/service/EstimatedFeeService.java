package com.swp.parking.service;

import com.swp.parking.dto.response.EstimatedFeeResponse;
import com.swp.parking.entity.ParkingOrder;
import com.swp.parking.entity.PricingRule;
import com.swp.parking.repository.ParkingOrderRepository;
import com.swp.parking.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Dịch vụ tính phí ước tính cho các đơn đỗ xe đang hoạt động của khách hàng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EstimatedFeeService {

    private final ParkingOrderRepository parkingOrderRepository;
    private final PricingRuleRepository pricingRuleRepository;

    /**
     * Trả về danh sách phí ước tính cho mỗi xe đang gửi (ACTIVE/CHECKED_IN) của user.
     */
    public List<EstimatedFeeResponse> getEstimatedFees(Long userId) {
        log.debug("Tính phí ước tính cho userId={}", userId);

        List<ParkingOrder> orders = parkingOrderRepository.findAllActiveOrdersByUserId(userId);

        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(this::buildEstimatedFeeResponse)
                .toList();
    }

    /**
     * Tính phí ước tính cho một đơn đỗ và map sang EstimatedFeeResponse.
     */
    private EstimatedFeeResponse buildEstimatedFeeResponse(ParkingOrder order) {
        LocalDateTime now = LocalDateTime.now();

        // Thời lượng gửi xe tính từ entryTime đến hiện tại
        long durationMinutes = ChronoUnit.MINUTES.between(order.getEntryTime(), now);
        LocalTime currentTime = LocalTime.now();

        // Xác định loại ngày: cuối tuần hoặc ngày thường
        String dayType = (LocalDate.now().getDayOfWeek() == DayOfWeek.SATURDAY
                || LocalDate.now().getDayOfWeek() == DayOfWeek.SUNDAY)
                ? "WEEKEND" : "WEEKDAY";

        Long parkingId = order.getParkingFacility().getParkingId();
        Long vehicleTypeId = order.getVehicle().getVehicleType().getVehicleTypeId();

        // Lấy rule active theo bãi + loại xe, ưu tiên cao nhất trước
        List<PricingRule> rules = pricingRuleRepository
                .findByParkingIdAndVehicleTypeIdAndIsActiveTrueOrderByPriorityDesc(
                        parkingId, vehicleTypeId);

        // Khớp rule theo khung giờ, loại ngày và khoảng phút gửi xe
        Optional<PricingRule> matched = rules.stream()
                .filter(r -> !currentTime.isBefore(r.getStartTime())
                        && !currentTime.isAfter(r.getEndTime())
                        && r.getDayType().equals(dayType)
                        && r.getMinMinutes() <= durationMinutes
                        && r.getMaxMinutes() >= durationMinutes)
                .findFirst();

        // Phí ước tính = đơn giá/phút × số phút; không khớp rule thì 0
        BigDecimal estimatedFee = matched
                .map(r -> r.getPrice().multiply(BigDecimal.valueOf(durationMinutes)))
                .orElse(BigDecimal.ZERO);

        return EstimatedFeeResponse.builder()
                .licensePlate(order.getLicensePlate())
                .entryTime(order.getEntryTime())
                .durationMinutes(durationMinutes)
                .vehicleTypeName(order.getVehicle().getVehicleType().getTypeName())
                .estimatedFee(estimatedFee)
                .calculatedAt(now)
                .build();
    }
}
