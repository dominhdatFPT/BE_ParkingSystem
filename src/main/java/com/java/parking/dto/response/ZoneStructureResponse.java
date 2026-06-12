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
public class ZoneStructureResponse {

    private Long zoneId;
    private String zoneName;
    private String vehicleTypeName;
    private String vehicleTypeCode;
    private Integer totalSlots;
    private List<SlotInfoResponse> slots;
}
