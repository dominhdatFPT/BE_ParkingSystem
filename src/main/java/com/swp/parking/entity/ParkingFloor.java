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

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng parking_floors – tầng đỗ xe thuộc một bãi (parking facility).
 */
@Entity
@Table(name = "parking_floors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParkingFloor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id")
    private Long floorId;

    // Tầng thuộc về một bãi đỗ xe
    @ManyToOne
    @JoinColumn(name = "parking_id")
    private ParkingFacility parkingFacility;

    @Column(name = "floor_name")
    private String floorName;

    @Column(name = "floor_number")
    private Integer floorNumber;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "current_vehicle_count")
    private Integer currentVehicleCount;

    @Column(name = "status")
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
