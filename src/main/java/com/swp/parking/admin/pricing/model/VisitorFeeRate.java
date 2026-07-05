package com.swp.parking.admin.pricing.model;

import com.swp.parking.model.ParkingFacility;
import com.swp.parking.model.VehicleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "visitor_fee_rates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitorFeeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_rate_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id")
    private ParkingFacility parkingFacility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    @Column(name = "first_block_minutes", nullable = false)
    private Integer firstBlockMinutes;

    @Column(name = "first_block_fee", nullable = false)
    private BigDecimal firstBlockFee;

    @Column(name = "next_block_minutes", nullable = false)
    private Integer nextBlockMinutes;

    @Column(name = "next_block_fee", nullable = false)
    private BigDecimal nextBlockFee;

    @Column(name = "daily_cap")
    private BigDecimal dailyCap;

    @Column(name = "overnight_fee", nullable = false)
    private BigDecimal overnightFee;

    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
