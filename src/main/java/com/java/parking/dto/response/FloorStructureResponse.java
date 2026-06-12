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
public class FloorStructureResponse {

    private Long floorId;
    private String floorName;
    private Integer floorNumber;
    private List<ZoneStructureResponse> zones;
}
