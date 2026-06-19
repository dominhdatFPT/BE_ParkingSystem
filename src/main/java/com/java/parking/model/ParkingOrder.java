package com.swp.parking.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_code")
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id")
    private ParkingFacility parkingFacility;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id")
    private ParkingFloor parkingFloor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private FeeSubscription subscription;

    @Column(name = "visitor_card_id")
    private Long visitorCardId;

    @Column(name = "entry_type")
    private String entryType;

    @Column(name = "license_plate")
    private String licensePlate;

    @Column(name = "entry_gate_id")
    private Long entryGateId;

    @Column(name = "card_id")
    private Long cardId;

    @Column(name = "entry_time")
    private LocalDateTime entryTime;

    @Column(name = "exit_time")
    private LocalDateTime exitTime;

    @Column(name = "calculated_fee", precision = 19, scale = 2)
    private BigDecimal calculatedFee;

    @Column(name = "parking_status")
    private String parkingStatus;

    @Column(name = "checked_in_by")
    private Long checkedInBy;

    @Column(name = "checked_out_by")
    private Long checkedOutBy;

    @Column(name = "checkout_confirmed_at")
    private LocalDateTime checkoutConfirmedAt;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "fee_rate_id")
    private Long feeRateId;

    @Column(name = "fee_breakdown", columnDefinition = "jsonb")
    private String feeBreakdown;

    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
