package com.swp.parking.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubscriptionRegisterRequest {

    @NotNull(message = "vehicleId không được để trống")
    private Long vehicleId;

    /** ID gói đăng ký user chọn (fee_package_id). */
    @NotNull(message = "planId không được để trống")
    private Long planId;

    /** true = bat tu dong gia han dinh ky. */
    private boolean autoRenew;
}
