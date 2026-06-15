package com.swp.parking.service;

import com.swp.parking.dto.response.ParkingAreaSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingAreaSummaryService {

    private final JdbcTemplate jdbcTemplate;

    public List<ParkingAreaSummaryResponse> getAreas(String buildingCode, Integer floorNumber) {
        StringBuilder sql = new StringBuilder("""
                SELECT area_id, building_code, building_name, floor_number, area_code,
                       vehicle_type, capacity, current_vehicle_count
                FROM parking_area_counts
                WHERE building_code IN ('LK', 'BH')
                """);
        List<Object> params = new ArrayList<>();

        if (buildingCode != null && !buildingCode.isBlank()) {
            sql.append(" AND building_code = ?");
            params.add(buildingCode);
        }

        if (floorNumber != null) {
            sql.append(" AND floor_number = ?");
            params.add(floorNumber);
        }

        sql.append(" ORDER BY building_code, floor_number, area_code");

        return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
            int capacity = rs.getInt("capacity");
            int currentVehicleCount = rs.getInt("current_vehicle_count");
            int occupancyPercent = capacity > 0
                    ? Math.round((currentVehicleCount * 100.0f) / capacity)
                    : 0;

            return ParkingAreaSummaryResponse.builder()
                    .id(rs.getLong("area_id"))
                    .buildingCode(rs.getString("building_code"))
                    .buildingName(rs.getString("building_name"))
                    .floorNumber(rs.getInt("floor_number"))
                    .areaCode(rs.getString("area_code"))
                    .vehicleType(rs.getString("vehicle_type"))
                    .capacity(capacity)
                    .currentVehicleCount(currentVehicleCount)
                    .occupancyPercent(occupancyPercent)
                    .build();
        }, params.toArray());
    }
}
