package com.swp.parking.dto.response;

import com.swp.parking.model.enums.ParkingSlotStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingOperationsResponse {

    private List<FacilityOption> facilities;
    private List<FloorOption> floors;
    private List<Slot> slots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FacilityOption {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FloorOption {
        private Long id;
        private Long facilityId;
        private String facilityName;
        private Integer floorNumber;
        private String floorName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Slot {
        private Long id;
        private String slotNumber;
        private Integer floor;
        private Long facilityId;
        private String facilityName;
        private Long floorId;
        private String floorName;
        private String areaKey;
        private ParkingSlotStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}
