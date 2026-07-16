package com.swp.parking.admin.pricing.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitorFeeRateResponse {

    private Long feeRateId;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private Integer firstBlockMinutes;
    private BigDecimal firstBlockFee;
    private Integer nextBlockMinutes;
    private BigDecimal nextBlockFee;
    private BigDecimal dailyCap;
    private BigDecimal overnightFee;
    private LocalDateTime effectiveFrom;
    private Boolean isActive;
}
