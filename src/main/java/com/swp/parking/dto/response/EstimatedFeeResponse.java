package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO phí ước tính cho một đơn đỗ xe đang hoạt động.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstimatedFeeResponse {

    private String licensePlate;
    private LocalDateTime entryTime;
    private Long durationMinutes;
    private String vehicleTypeName;
    private BigDecimal estimatedFee;
    private LocalDateTime calculatedAt;
    private String note;
}
