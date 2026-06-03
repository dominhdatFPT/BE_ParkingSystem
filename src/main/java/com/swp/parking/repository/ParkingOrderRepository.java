package com.swp.parking.repository;

import com.swp.parking.entity.ParkingOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository truy vấn dữ liệu phiên gửi xe (parking_orders).
 */
public interface ParkingOrderRepository extends JpaRepository<ParkingOrder, Long> {

    /**
     * Lấy tất cả đơn đỗ xe đang hoạt động của user (qua chuỗi vehicle → customer → user).
     * Chỉ lấy trạng thái ACTIVE hoặc CHECKED_IN, sắp xếp mới nhất trước.
     */
    @Query("""
            SELECT o FROM ParkingOrder o
            WHERE o.vehicle.customer.user.userId = :userId
            AND o.parkingStatus IN ('ACTIVE', 'CHECKED_IN')
            ORDER BY o.entryTime DESC
            """)
    List<ParkingOrder> findAllActiveOrdersByUserId(@Param("userId") Long userId);
}
