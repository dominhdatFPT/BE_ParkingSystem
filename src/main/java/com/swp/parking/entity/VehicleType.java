package com.swp.parking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity ánh xạ bảng vehicle_types – danh mục loại phương tiện (ô tô, xe máy, ...).
 */
@Entity
@Table(name = "vehicle_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "type_code")
    private String typeCode;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
