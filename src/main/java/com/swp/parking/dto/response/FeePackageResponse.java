package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePackageResponse {

    private Long id;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String name;
    private Integer durationMonths;
    private List<String> benefits;
    private Boolean isPopular;
    private Boolean isBestValue;
    private Boolean isActive;
    private BigDecimal currentPrice;
    private BigDecimal price;
    private BigDecimal originalPrice;
    private Integer discountPercent;
    private Long priceHistoryId;
    private LocalDateTime effectiveFrom;
}
