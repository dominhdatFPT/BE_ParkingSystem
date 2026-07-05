package com.swp.parking.admin.pricing.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePriceRequest {

    @NotNull(message = "Giá gốc không được để trống")
    @DecimalMin(value = "0.01", message = "Giá gốc phải lớn hơn 0")
    private BigDecimal originalPrice;

    @NotNull(message = "Giá bán không được để trống")
    @DecimalMin(value = "0.01", message = "Giá bán phải lớn hơn 0")
    private BigDecimal price;

    @Min(value = 0, message = "Phần trăm giảm giá phải >= 0")
    @Max(value = 100, message = "Phần trăm giảm giá phải <= 100")
    private Integer discountPercent;
}
