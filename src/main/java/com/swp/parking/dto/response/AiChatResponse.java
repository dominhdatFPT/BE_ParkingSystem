package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiChatResponse {

    private String sessionId;
    private String reply;
    private String status;
    private Long registrationId;
    private CollectedData collectedData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CollectedData {
        private String ownerName;
        private String phone;
        private String vehicleType;
        private String licensePlate;
        private String packageType;
    }
}
