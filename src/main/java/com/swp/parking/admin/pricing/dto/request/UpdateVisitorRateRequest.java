package com.swp.parking.admin.pricing.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
public class UpdateVisitorRateRequest {

    @NotNull(message = "Số phút block đầu tiên không được để trống")
    @Min(value = 1, message = "Số phút block đầu tiên phải lớn hơn 0")
    private Integer firstBlockMinutes;

    @NotNull(message = "Phí block đầu tiên không được để trống")
    @DecimalMin(value = "0.01", message = "Phí block đầu tiên phải lớn hơn 0")
    private BigDecimal firstBlockFee;

    @NotNull(message = "Số phút block tiếp theo không được để trống")
    @Min(value = 1, message = "Số phút block tiếp theo phải lớn hơn 0")
    private Integer nextBlockMinutes;

    @NotNull(message = "Phí block tiếp theo không được để trống")
    @DecimalMin(value = "0.01", message = "Phí block tiếp theo phải lớn hơn 0")
    private BigDecimal nextBlockFee;

    @DecimalMin(value = "0.01", message = "Giới hạn ngày phải lớn hơn 0")
    private BigDecimal dailyCap;

    @NotNull(message = "Phí qua đêm không được để trống")
    @DecimalMin(value = "0.01", message = "Phí qua đêm phải lớn hơn 0")
    private BigDecimal overnightFee;
}
