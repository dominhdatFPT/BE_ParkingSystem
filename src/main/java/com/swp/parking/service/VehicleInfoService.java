package com.swp.parking.service;

import com.swp.parking.dto.response.VehicleInfoResponse;
import com.swp.parking.entity.ParkingOrder;
import com.swp.parking.repository.ParkingOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Dịch vụ lấy danh sách xe đang gửi của khách hàng đã đăng nhập.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleInfoService {

    private final ParkingOrderRepository parkingOrderRepository;

    /**
     * Trả về danh sách xe đang đỗ (phiên ACTIVE/CHECKED_IN) của user.
     * Một khách có thể gửi nhiều xe cùng lúc; không có đơn thì trả list rỗng.
     */
    public List<VehicleInfoResponse> getActiveVehicles(Long userId) {
        log.debug("Lấy danh sách xe đang gửi cho userId={}", userId);

        List<ParkingOrder> orders = parkingOrderRepository.findAllActiveOrdersByUserId(userId);

        // Khách chưa gửi xe nào – trả về rỗng, không ném exception
        if (orders.isEmpty()) {
            return Collections.emptyList();
        }

        return orders.stream()
                .map(this::mapToVehicleInfoResponse)
                .toList();
    }

    /**
     * Ánh xạ một ParkingOrder sang VehicleInfoResponse cho client.
     */
    private VehicleInfoResponse mapToVehicleInfoResponse(ParkingOrder order) {
        // Kiểm tra tầng đỗ: null thì gán giá trị mặc định
        String floorName = order.getParkingFloor() != null
                ? order.getParkingFloor().getFloorName() : "Chưa xác định";
        Integer floorNumber = order.getParkingFloor() != null
                ? order.getParkingFloor().getFloorNumber() : null;

        return VehicleInfoResponse.builder()
                .licensePlate(order.getLicensePlate())
                .brand(order.getVehicle().getBrand())
                .color(order.getVehicle().getColor())
                .vehicleTypeName(order.getVehicle().getVehicleType().getTypeName())
                .parkingStatus(order.getParkingStatus())
                .parkingName(order.getParkingFacility().getParkingName())
                .floorName(floorName)
                .floorNumber(floorNumber)
                .entryTime(order.getEntryTime())
                .build();
    }
}
