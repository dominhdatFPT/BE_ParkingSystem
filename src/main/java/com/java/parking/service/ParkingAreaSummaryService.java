package com.swp.parking.service;

import com.swp.parking.dto.response.ParkingAreaSummaryResponse;
import com.swp.parking.dto.response.ParkingAreaOptionsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ParkingAreaSummaryService {

    private final JdbcTemplate jdbcTemplate;

    public ParkingAreaOptionsResponse getOptions() {
        String sql = """
                SELECT
                    pf.parking_id,
                    pf.parking_name,
                    CASE
                        WHEN LOWER(b.email) = 'cammy@sps.vn' THEN 'TCM'
                        WHEN LOWER(b.email) = 'bienhoa@sps.vn' THEN 'BH'
                        ELSE 'LK'
                    END AS building_code,
                    ARRAY_AGG(DISTINCT COALESCE(fl.floor_number, ps.floor) ORDER BY COALESCE(fl.floor_number, ps.floor)) AS floors
                FROM parking_facilities pf
                JOIN buildings b ON b.building_id = pf.building_id
                LEFT JOIN parking_floors fl ON fl.parking_id = pf.parking_id
                LEFT JOIN parking_slots ps ON ps.parking_id = pf.parking_id
                WHERE COALESCE(fl.floor_number, ps.floor) IS NOT NULL
                GROUP BY pf.parking_id, pf.parking_name, building_code
                ORDER BY pf.parking_id
                """;

        try {
            List<ParkingAreaOptionsResponse.BuildingOption> buildings = jdbcTemplate.query(sql, (rs, rowNum) ->
                    ParkingAreaOptionsResponse.BuildingOption.builder()
                            .code(rs.getString("building_code"))
                            .name(rs.getString("parking_name"))
                            .parkingId(rs.getObject("parking_id", Long.class))
                            .floors(List.of((Integer[]) rs.getArray("floors").getArray()))
                            .build()
            );

            return ParkingAreaOptionsResponse.builder()
                    .buildings(buildings)
                    .build();
        } catch (DataAccessException ex) {
            log.error("Could not read parking area options from database: {}", ex.getMessage(), ex);
            return ParkingAreaOptionsResponse.builder()
                    .buildings(List.of())
                    .build();
        }
    }

    public List<ParkingAreaSummaryResponse> getAreas(String buildingCode, Integer floorNumber) {
        if (floorNumber != null && floorNumber < 1) {
            log.warn("Invalid parking area summary floorNumber: {}", floorNumber);
            return List.of();
        }

        return getAreasFromParkingSlotsSafely(buildingCode, floorNumber);
    }

    private List<ParkingAreaSummaryResponse> getAreasFromParkingSlotsSafely(String buildingCode, Integer floorNumber) {
        try {
            return getAreasFromParkingSlots(buildingCode, floorNumber);
        } catch (DataAccessException ex) {
            log.error("Could not build parking area summary from parking_slots. buildingCode={}, floorNumber={}, error={}",
                    buildingCode, floorNumber, ex.getMessage(), ex);
            return List.of();
        }
    }

    private List<ParkingAreaSummaryResponse> getAreasFromParkingSlots(String buildingCode, Integer floorNumber) {
        String normalizedBuildingCode = normalizeBuildingCode(buildingCode);
        String buildingEmail = resolveBuildingEmail(normalizedBuildingCode);

        String sql = """
                WITH area_codes(area_code) AS (
                    VALUES ('A'), ('B'), ('C'), ('D')
                ),
                selected_facility AS (
                    SELECT
                        pf.parking_id,
                        pf.parking_name,
                        ? AS building_code
                    FROM parking_facilities pf
                    JOIN buildings b ON b.building_id = pf.building_id
                    WHERE LOWER(b.email) = ?
                    LIMIT 1
                ),
                slot_areas AS (
                    SELECT
                        ps.id,
                        sf.building_code,
                        sf.parking_name AS building_name,
                        ps.floor AS floor_number,
                        ps.status,
                        COALESCE(
                            SUBSTRING(ps.slot_number FROM '^[A-Za-z]+-([A-Za-z])'),
                            SUBSTRING(ps.slot_number FROM '^([A-Za-z])'),
                            'A'
                        ) AS area_code,
                        CASE
                            WHEN ps.slot_number LIKE 'M-%' THEN 'MOTORBIKE'
                            WHEN SUBSTRING(ps.slot_number FROM '^[A-Za-z]+-([A-Za-z])') IN ('C', 'D') THEN 'MOTORBIKE'
                            WHEN SUBSTRING(ps.slot_number FROM '^([A-Za-z])') IN ('C', 'D') THEN 'MOTORBIKE'
                            ELSE 'CAR'
                        END AS vehicle_type
                    FROM parking_slots ps
                    JOIN selected_facility sf ON sf.parking_id = ps.parking_id
                    WHERE (? IS NULL OR ps.floor = ?)
                ),
                slot_summary AS (
                    SELECT
                        building_code,
                        building_name,
                        floor_number,
                        area_code,
                        CASE
                            WHEN COUNT(*) FILTER (WHERE vehicle_type = 'MOTORBIKE') > COUNT(*) FILTER (WHERE vehicle_type = 'CAR')
                                THEN 'MOTORBIKE'
                            ELSE 'CAR'
                        END AS vehicle_type,
                        COUNT(*)::int AS capacity,
                        COUNT(*) FILTER (WHERE status IN ('OCCUPIED', 'RESERVED'))::int AS current_vehicle_count,
                        COUNT(*) FILTER (WHERE status = 'AVAILABLE')::int AS available_count,
                        COUNT(*) FILTER (WHERE status = 'MAINTENANCE')::int AS maintenance_count,
                        MIN(id) AS area_id
                    FROM slot_areas
                    GROUP BY building_code, building_name, floor_number, area_code
                )
                SELECT
                    COALESCE(ss.area_id, ROW_NUMBER() OVER (ORDER BY ac.area_code) * -1) AS area_id,
                    sf.building_code,
                    sf.parking_name AS building_name,
                    ? AS floor_number,
                    ac.area_code,
                    COALESCE(ss.vehicle_type, CASE WHEN ac.area_code IN ('C', 'D') THEN 'MOTORBIKE' ELSE 'CAR' END) AS vehicle_type,
                    COALESCE(ss.capacity, 0) AS capacity,
                    COALESCE(ss.current_vehicle_count, 0) AS current_vehicle_count,
                    COALESCE(ss.available_count, 0) AS available_count,
                    COALESCE(ss.maintenance_count, 0) AS maintenance_count
                FROM selected_facility sf
                CROSS JOIN area_codes ac
                LEFT JOIN slot_summary ss ON ss.area_code = ac.area_code
                ORDER BY ac.area_code
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> mapAreaSummary(
                rs.getObject("area_id", Long.class),
                rs.getString("building_code"),
                rs.getString("building_name"),
                rs.getObject("floor_number", Integer.class),
                rs.getString("area_code"),
                rs.getString("vehicle_type"),
                rs.getObject("capacity", Integer.class),
                rs.getObject("current_vehicle_count", Integer.class),
                rs.getObject("available_count", Integer.class),
                rs.getObject("maintenance_count", Integer.class)
        ), normalizedBuildingCode, buildingEmail, floorNumber, floorNumber, floorNumber);
    }

    private ParkingAreaSummaryResponse mapAreaSummary(
            Long id,
            String buildingCode,
            String buildingName,
            Integer floorNumber,
            String areaCode,
            String vehicleType,
            Integer capacity,
            Integer currentVehicleCount,
            Integer availableCount,
            Integer maintenanceCount
    ) {
        int safeCapacity = capacity != null ? capacity : 0;
        int safeCurrentVehicleCount = currentVehicleCount != null ? currentVehicleCount : 0;
        int safeAvailableCount = availableCount != null ? availableCount : 0;
        int safeMaintenanceCount = maintenanceCount != null ? maintenanceCount : 0;
        int occupancyPercent = safeCapacity > 0
                ? Math.round((safeCurrentVehicleCount * 100.0f) / safeCapacity)
                : 0;

        return ParkingAreaSummaryResponse.builder()
                .id(id)
                .buildingCode(buildingCode)
                .buildingName(buildingName)
                .floorNumber(floorNumber)
                .areaCode(areaCode)
                .vehicleType(vehicleType)
                .capacity(safeCapacity)
                .currentVehicleCount(safeCurrentVehicleCount)
                .availableCount(safeAvailableCount)
                .maintenanceCount(safeMaintenanceCount)
                .occupancyPercent(occupancyPercent)
                .build();
    }

    private String normalizeBuildingCode(String buildingCode) {
        return buildingCode != null && !buildingCode.isBlank()
                ? buildingCode.trim().toUpperCase()
                : "LK";
    }

    private String resolveBuildingEmail(String buildingCode) {
        return switch (buildingCode) {
            case "TCM" -> "cammy@sps.vn";
            case "BH" -> "bienhoa@sps.vn";
            default -> "longkhanh@sps.vn";
        };
    }
}
