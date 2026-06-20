package com.swp.parking.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Getter
public class VehicleAlreadyHasActiveSubscriptionException extends AppException {

    private final LocalDateTime currentSubscriptionEndDate;

    public VehicleAlreadyHasActiveSubscriptionException(LocalDateTime currentSubscriptionEndDate) {
        super(HttpStatus.CONFLICT, "Xe này đã có gói đang hoạt động");
        this.currentSubscriptionEndDate = currentSubscriptionEndDate;
    }
}
