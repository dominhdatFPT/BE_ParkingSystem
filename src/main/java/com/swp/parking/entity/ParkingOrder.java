package com.swp.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng parking_orders – phiên gửi xe (đơn đỗ) của một phương tiện.
 */
@Entity
@Table(name = "parking_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    // Xe đang được gửi trong phiên này
    @ManyToOne
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    // Bãi đỗ nơi xe đang đỗ
    @ManyToOne
    @JoinColumn(name = "parking_id")
    private ParkingFacility parkingFacility;

    // Tầng cụ thể trong bãi
    @ManyToOne
    @JoinColumn(name = "floor_id")
    private ParkingFloor parkingFloor;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "calculated_fee")
    private BigDecimal calculatedFee;

    @Column(name = "final_fee")
    private BigDecimal finalFee;

    // Trạng thái phiên gửi: ACTIVE, CHECKED_IN, COMPLETED, ...
    @Column(name = "parking_status")
    private String parkingStatus;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
