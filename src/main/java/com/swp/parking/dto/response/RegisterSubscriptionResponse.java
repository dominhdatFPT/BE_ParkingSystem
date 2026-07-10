package com.swp.parking.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegisterSubscriptionResponse {

    private Long subscriptionId;
    private Long invoiceId;

    private String message;
}
