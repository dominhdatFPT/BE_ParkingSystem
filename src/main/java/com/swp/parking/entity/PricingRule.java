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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity ánh xạ bảng pricing_rules – quy tắc tính giá theo bãi, loại xe và khung giờ.
 */
@Entity
@Table(name = "pricing_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PricingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pricing_rule_id")
    private Long pricingRuleId;

    @Column(name = "parking_id")
    private Long parkingId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @Column(name = "rule_name")
    private String ruleName;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "day_type")
    private String dayType;

    @Column(name = "min_minutes")
    private Integer minMinutes;

    @Column(name = "max_minutes")
    private Integer maxMinutes;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
