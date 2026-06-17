package com.swp.parking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParkingAreaOptionsResponse {

    private List<BuildingOption> buildings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BuildingOption {
        private String code;
        private String name;
        private Long parkingId;
        private List<Integer> floors;
    }
}
