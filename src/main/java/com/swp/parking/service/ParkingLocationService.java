package com.swp.parking.service;

import com.swp.parking.dto.response.ParkingLocationResponse;
import com.swp.parking.entity.ParkingOrder;
import com.swp.parking.repository.ParkingOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

/**
 * Dịch vụ lấy vị trí đỗ và thời lượng gửi xe cho các phương tiện đang hoạt động.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingLocationService {

    private final ParkingOrderRepository parkingOrderRepository;

    /**
     * Trả về danh sách vị trí đỗ (bãi + tầng) và số phút đã gửi cho mỗi xe active của user.
     */
    public List<ParkingLocationResponse> getParkingLocations(Long userId) {
        log.debug("Lấy vị trí đỗ xe đang gửi cho userId={}", userId);

        List<ParkingOrder> orders = parkingOrderRepository.findAllActiveOrdersByUserId(userId);

        // Không có đơn đang active – trả list rỗng
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(this::mapToParkingLocationResponse)
                .toList();
    }

    /**
     * Ánh xạ ParkingOrder sang ParkingLocationResponse, tính duration từ entryTime đến hiện tại.
     */
    private ParkingLocationResponse mapToParkingLocationResponse(ParkingOrder order) {
        LocalDateTime now = LocalDateTime.now();

        // Thời lượng gửi xe tính bằng phút từ lúc vào bãi đến thời điểm hiện tại
        // entryTime null thì coi như 0 phút
        long durationMinutes = order.getEntryTime() != null
                ? ChronoUnit.MINUTES.between(order.getEntryTime(), now) : 0L;

        // Kiểm tra tầng đỗ: null thì gán giá trị mặc định
        String floorName = order.getParkingFloor() != null
                ? order.getParkingFloor().getFloorName() : "Chưa xác định";
        Integer floorNumber = order.getParkingFloor() != null
                ? order.getParkingFloor().getFloorNumber() : null;

        return ParkingLocationResponse.builder()
                .licensePlate(order.getLicensePlate())
                .parkingName(order.getParkingFacility().getParkingName())
                .floorName(floorName)
                .floorNumber(floorNumber)
                .entryTime(order.getEntryTime())
                .durationMinutes(durationMinutes)
                .build();
    }
}
