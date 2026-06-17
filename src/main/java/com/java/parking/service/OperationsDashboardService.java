package com.swp.parking.service;

import com.swp.parking.dto.response.OperationsDashboardResponse;
import com.swp.parking.dto.response.ParkingOperationsResponse;
import com.swp.parking.model.enums.ParkingSlotStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperationsDashboardService {

    private static final String DEFAULT_FACILITY_NAME = "Long Khánh";

    private final JdbcTemplate jdbcTemplate;

    public ParkingOperationsResponse getParkingOperations() {
        List<ParkingOperationsResponse.Slot> slots = readOperationSlots();
        List<ParkingOperationsResponse.FacilityOption> facilities = readFacilitiesFromSlots(slots);
        List<ParkingOperationsResponse.FloorOption> floors = readFloorsFromSlots(slots);

        return ParkingOperationsResponse.builder()
                .facilities(facilities)
                .floors(floors)
                .slots(slots)
                .build();
    }

    public OperationsDashboardResponse getDashboard() {
        List<ParkingOperationsResponse.Slot> slots = readOperationSlots();
        long totalSlots = slots.size();
        long availableSlots = slots.stream().filter(slot -> slot.getStatus() == ParkingSlotStatus.AVAILABLE).count();
        long vehiclesInParking = countActiveParkingOrders();
        if (vehiclesInParking == 0) {
            vehiclesInParking = slots.stream()
                    .filter(slot -> slot.getStatus() == ParkingSlotStatus.OCCUPIED || slot.getStatus() == ParkingSlotStatus.RESERVED)
                    .count();
        }

        OperationsDashboardResponse.Metrics metrics = OperationsDashboardResponse.Metrics.builder()
                .vehiclesInParking(vehiclesInParking)
                .availableSlots(availableSlots)
                .pendingBookings(countPendingBookings())
                .vehiclesInToday(countVehiclesInToday())
                .vehiclesOutToday(countVehiclesOutToday())
                .openIncidents(countOpenIncidents())
                .revenueToday(sumRevenueToday())
                .totalSlots(totalSlots)
                .occupancyRate(totalSlots > 0 ? (int) Math.round((vehiclesInParking * 100.0) / totalSlots) : 0)
                .build();

        return OperationsDashboardResponse.builder()
                .metrics(metrics)
                .areaOccupancy(buildAreaOccupancy(slots))
                .trafficByHour(readTrafficByHour())
                .pendingBookings(readPendingBookings())
                .recentIncidents(readRecentIncidents())
                .recentVehicleActivities(readRecentVehicleActivities())
                .build();
    }

    private List<ParkingOperationsResponse.Slot> readOperationSlots() {
        String sql = """
                WITH default_facility AS (
                    SELECT parking_id, parking_name
                    FROM parking_facilities
                    ORDER BY parking_id
                    LIMIT 1
                ),
                first_floor AS (
                    SELECT DISTINCT ON (floor_number)
                        floor_id,
                        floor_name,
                        floor_number,
                        parking_id
                    FROM parking_floors
                    ORDER BY floor_number, parking_id
                )
                SELECT
                    ps.id,
                    ps.slot_number,
                    ps.floor,
                    ps.status,
                    ps.created_at,
                    ps.updated_at,
                    fl.floor_id,
                    fl.floor_name,
                    fl.floor_number,
                    COALESCE(pf.parking_id, df.parking_id) AS parking_id,
                    COALESCE(pf.parking_name, df.parking_name) AS parking_name
                FROM parking_slots ps
                LEFT JOIN first_floor ff ON ps.floor_id IS NULL AND ff.floor_number = ps.floor
                LEFT JOIN parking_floors fl ON fl.floor_id = COALESCE(ps.floor_id, ff.floor_id)
                LEFT JOIN parking_facilities pf ON pf.parking_id = COALESCE(ps.parking_id, fl.parking_id)
                LEFT JOIN default_facility df ON true
                ORDER BY COALESCE(pf.parking_id, df.parking_id, 0), ps.floor, ps.slot_number
                """;

        try {
            List<ParkingOperationsResponse.Slot> slots = jdbcTemplate.query(sql, this::mapOperationSlot);
            if (slots.isEmpty()) {
                return List.of();
            }
            return slots;
        } catch (DataAccessException ex) {
            log.error("Could not read parking operations slots from database: {}", ex.getMessage(), ex);
            return List.of();
        }
    }

    private ParkingOperationsResponse.Slot mapOperationSlot(ResultSet rs, int rowNum) throws SQLException {
        String slotNumber = rs.getString("slot_number");
        Integer floorNumber = getInteger(rs, "floor");
        String facilityName = valueOrDefault(rs.getString("parking_name"), DEFAULT_FACILITY_NAME);
        String areaKey = resolveAreaKey(slotNumber, rowNum);
        ParkingSlotStatus status = parseSlotStatus(rs.getString("status"));

        return ParkingOperationsResponse.Slot.builder()
                .id(getLong(rs, "id"))
                .slotNumber(slotNumber)
                .floor(floorNumber)
                .facilityId(getLong(rs, "parking_id"))
                .facilityName(facilityName)
                .floorId(getLong(rs, "floor_id"))
                .floorName(valueOrDefault(rs.getString("floor_name"), floorNumber != null ? "Tầng " + floorNumber : "Tầng 1"))
                .areaKey(areaKey)
                .status(status)
                .createdAt(getLocalDateTime(rs, "created_at"))
                .updatedAt(getLocalDateTime(rs, "updated_at"))
                .build();
    }

    private List<ParkingOperationsResponse.FacilityOption> readFacilitiesFromSlots(List<ParkingOperationsResponse.Slot> slots) {
        Map<String, ParkingOperationsResponse.FacilityOption> byName = new LinkedHashMap<>();
        for (ParkingOperationsResponse.Slot slot : slots) {
            String name = valueOrDefault(slot.getFacilityName(), DEFAULT_FACILITY_NAME);
            byName.putIfAbsent(name, ParkingOperationsResponse.FacilityOption.builder()
                    .id(slot.getFacilityId())
                    .name(name)
                    .build());
        }
        if (byName.isEmpty()) {
            byName.put(DEFAULT_FACILITY_NAME, ParkingOperationsResponse.FacilityOption.builder()
                    .name(DEFAULT_FACILITY_NAME)
                    .build());
        }
        return new ArrayList<>(byName.values());
    }

    private List<ParkingOperationsResponse.FloorOption> readFloorsFromSlots(List<ParkingOperationsResponse.Slot> slots) {
        Map<String, ParkingOperationsResponse.FloorOption> byKey = new LinkedHashMap<>();
        for (ParkingOperationsResponse.Slot slot : slots) {
            String key = valueOrDefault(slot.getFacilityName(), DEFAULT_FACILITY_NAME) + ":" + slot.getFloor();
            byKey.putIfAbsent(key, ParkingOperationsResponse.FloorOption.builder()
                    .id(slot.getFloorId())
                    .facilityId(slot.getFacilityId())
                    .facilityName(valueOrDefault(slot.getFacilityName(), DEFAULT_FACILITY_NAME))
                    .floorNumber(slot.getFloor())
                    .floorName(valueOrDefault(slot.getFloorName(), "Tầng " + slot.getFloor()))
                    .build());
        }
        return new ArrayList<>(byKey.values());
    }

    private List<OperationsDashboardResponse.AreaOccupancy> buildAreaOccupancy(List<ParkingOperationsResponse.Slot> slots) {
        Map<String, AreaCounter> counters = new LinkedHashMap<>();
        for (ParkingOperationsResponse.Slot slot : slots) {
            String facilityName = valueOrDefault(slot.getFacilityName(), DEFAULT_FACILITY_NAME);
            String areaKey = valueOrDefault(slot.getAreaKey(), "A");
            String key = facilityName + ":" + slot.getFloor() + ":" + areaKey;
            AreaCounter counter = counters.computeIfAbsent(key, ignored -> new AreaCounter(facilityName, slot.getFloor(), areaKey));
            counter.total++;
            if (slot.getStatus() == ParkingSlotStatus.AVAILABLE) {
                counter.available++;
            } else {
                counter.occupied++;
            }
        }

        return counters.values().stream()
                .map(counter -> OperationsDashboardResponse.AreaOccupancy.builder()
                        .name("Tầng " + counter.floorNumber + " - Khu " + counter.areaKey)
                        .facilityName(counter.facilityName)
                        .floorNumber(counter.floorNumber)
                        .areaKey(counter.areaKey)
                        .total(counter.total)
                        .occupied(counter.occupied)
                        .available(counter.available)
                        .fillRate(counter.total > 0 ? (int) Math.round((counter.occupied * 100.0) / counter.total) : 0)
                        .build())
                .toList();
    }

    private long countPendingBookings() {
        return queryLong("""
                SELECT COUNT(*)
                FROM bookings
                WHERE status IN ('WAITING_STAFF_APPROVAL', 'PENDING')
                """);
    }

    private long countActiveParkingOrders() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE parking_status = 'ACTIVE'
                   OR (entry_time IS NOT NULL AND exit_time IS NULL)
                """);
    }

    private long countVehiclesInToday() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE entry_time >= CURRENT_DATE
                  AND entry_time < CURRENT_DATE + INTERVAL '1 day'
                """);
    }

    private long countVehiclesOutToday() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE exit_time >= CURRENT_DATE
                  AND exit_time < CURRENT_DATE + INTERVAL '1 day'
                """);
    }

    private long countOpenIncidents() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE COALESCE(notes, '') <> ''
                  AND COALESCE(parking_status, '') IN ('ISSUE', 'EXCEPTION', 'OPEN', 'ACTIVE')
                """);
    }

    private BigDecimal sumRevenueToday() {
        String sql = """
                SELECT COALESCE(SUM(calculated_fee), 0)
                FROM parking_orders
                WHERE calculated_fee IS NOT NULL
                  AND exit_time >= CURRENT_DATE
                  AND exit_time < CURRENT_DATE + INTERVAL '1 day'
                """;
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
            return value != null ? value : BigDecimal.ZERO;
        } catch (DataAccessException ex) {
            log.warn("Could not calculate today revenue: {}", ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<OperationsDashboardResponse.TrafficPoint> readTrafficByHour() {
        List<OperationsDashboardResponse.TrafficPoint> points = new ArrayList<>();
        for (int hour = 0; hour < 24; hour += 2) {
            points.add(OperationsDashboardResponse.TrafficPoint.builder()
                    .hour(String.format("%02d:00", hour))
                    .in(0)
                    .out(0)
                    .build());
        }

        mergeTraffic(points, "in", """
                SELECT FLOOR(EXTRACT(HOUR FROM entry_time) / 2)::int AS bucket, COUNT(*) AS count
                FROM parking_orders
                WHERE entry_time >= CURRENT_DATE
                  AND entry_time < CURRENT_DATE + INTERVAL '1 day'
                GROUP BY bucket
                """);
        mergeTraffic(points, "out", """
                SELECT FLOOR(EXTRACT(HOUR FROM exit_time) / 2)::int AS bucket, COUNT(*) AS count
                FROM parking_orders
                WHERE exit_time >= CURRENT_DATE
                  AND exit_time < CURRENT_DATE + INTERVAL '1 day'
                GROUP BY bucket
                """);

        return points;
    }

    private void mergeTraffic(List<OperationsDashboardResponse.TrafficPoint> points, String type, String sql) {
        try {
            jdbcTemplate.query(sql, rs -> {
                int bucket = rs.getInt("bucket");
                if (bucket >= 0 && bucket < points.size()) {
                    OperationsDashboardResponse.TrafficPoint point = points.get(bucket);
                    long count = rs.getLong("count");
                    if ("in".equals(type)) {
                        point.setIn(count);
                    } else {
                        point.setOut(count);
                    }
                }
            });
        } catch (DataAccessException ex) {
            log.warn("Could not read traffic {} by hour: {}", type, ex.getMessage());
        }
    }

    private List<OperationsDashboardResponse.RecentBooking> readPendingBookings() {
        String sql = """
                WITH first_floor AS (
                    SELECT DISTINCT ON (floor_number)
                        floor_id,
                        floor_name,
                        floor_number,
                        parking_id
                    FROM parking_floors
                    ORDER BY floor_number, parking_id
                )
                SELECT
                    b.id,
                    u.user_id,
                    u.full_name,
                    ps.slot_number,
                    pf.parking_name,
                    fl.floor_name,
                    b.status,
                    b.start_time,
                    b.end_time,
                    b.created_at
                FROM bookings b
                LEFT JOIN users u ON u.user_id = b.user_id
                LEFT JOIN parking_slots ps ON ps.id = b.parking_slot_id
                LEFT JOIN first_floor fl ON fl.floor_number = ps.floor
                LEFT JOIN parking_facilities pf ON pf.parking_id = fl.parking_id
                WHERE b.status IN ('WAITING_STAFF_APPROVAL', 'PENDING')
                ORDER BY b.created_at DESC
                LIMIT 8
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> OperationsDashboardResponse.RecentBooking.builder()
                    .id(getLong(rs, "id"))
                    .userId(getLong(rs, "user_id"))
                    .userFullName(rs.getString("full_name"))
                    .slotNumber(rs.getString("slot_number"))
                    .parkingName(rs.getString("parking_name"))
                    .floorName(rs.getString("floor_name"))
                    .status(rs.getString("status"))
                    .startTime(getLocalDateTime(rs, "start_time"))
                    .endTime(getLocalDateTime(rs, "end_time"))
                    .createdAt(getLocalDateTime(rs, "created_at"))
                    .build());
        } catch (DataAccessException ex) {
            log.warn("Could not read pending bookings for dashboard: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<OperationsDashboardResponse.RecentIncident> readRecentIncidents() {
        String sql = """
                SELECT
                    order_id,
                    license_plate,
                    parking_status,
                    notes,
                    created_at
                FROM parking_orders
                WHERE COALESCE(notes, '') <> ''
                ORDER BY created_at DESC
                LIMIT 5
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> OperationsDashboardResponse.RecentIncident.builder()
                    .id(getLong(rs, "order_id"))
                    .type("PARKING_ORDER_NOTE")
                    .title("Ghi chú vận hành")
                    .description(rs.getString("notes"))
                    .status(rs.getString("parking_status"))
                    .licensePlate(rs.getString("license_plate"))
                    .createdAt(getLocalDateTime(rs, "created_at"))
                    .build());
        } catch (DataAccessException ex) {
            log.warn("Could not read recent incidents from parking_orders: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<OperationsDashboardResponse.VehicleActivity> readRecentVehicleActivities() {
        String sql = """
                SELECT
                    po.order_id,
                    po.order_code,
                    po.license_plate,
                    po.entry_time,
                    po.exit_time,
                    po.parking_status,
                    po.calculated_fee,
                    po.updated_at,
                    pf.parking_name,
                    fl.floor_name
                FROM parking_orders po
                LEFT JOIN parking_facilities pf ON pf.parking_id = po.parking_id
                LEFT JOIN parking_floors fl ON fl.floor_id = po.floor_id
                ORDER BY po.updated_at DESC
                LIMIT 8
                """;
        try {
            return jdbcTemplate.query(sql, (rs, rowNum) -> OperationsDashboardResponse.VehicleActivity.builder()
                    .id(getLong(rs, "order_id"))
                    .orderCode(rs.getString("order_code"))
                    .licensePlate(rs.getString("license_plate"))
                    .parkingName(rs.getString("parking_name"))
                    .floorName(rs.getString("floor_name"))
                    .status(rs.getString("parking_status"))
                    .entryTime(getLocalDateTime(rs, "entry_time"))
                    .exitTime(getLocalDateTime(rs, "exit_time"))
                    .calculatedFee(rs.getBigDecimal("calculated_fee"))
                    .updatedAt(getLocalDateTime(rs, "updated_at"))
                    .build());
        } catch (DataAccessException ex) {
            log.warn("Could not read recent vehicle activities: {}", ex.getMessage());
            return List.of();
        }
    }

    private long queryLong(String sql) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class);
            return value != null ? value : 0;
        } catch (DataAccessException ex) {
            log.warn("Could not read dashboard metric: {}", ex.getMessage());
            return 0;
        }
    }

    private ParkingSlotStatus parseSlotStatus(String status) {
        try {
            return ParkingSlotStatus.valueOf(valueOrDefault(status, "AVAILABLE"));
        } catch (IllegalArgumentException ex) {
            return ParkingSlotStatus.AVAILABLE;
        }
    }

    private String resolveAreaKey(String slotNumber, int rowNum) {
        if (slotNumber != null) {
            String upper = slotNumber.toUpperCase();
            for (String area : List.of("A", "B", "C", "D")) {
                if (upper.matches(".*(^|[-_\\s])" + area + "([0-9]|[-_\\s]).*") || upper.startsWith(area)) {
                    return area;
                }
            }
        }
        return List.of("A", "B", "C", "D").get(Math.floorMod(rowNum, 4));
    }

    private String valueOrDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp != null ? timestamp.toLocalDateTime() : null;
    }

    private static class AreaCounter {
        private final String facilityName;
        private final Integer floorNumber;
        private final String areaKey;
        private long total;
        private long occupied;
        private long available;

        private AreaCounter(String facilityName, Integer floorNumber, String areaKey) {
            this.facilityName = facilityName;
            this.floorNumber = floorNumber;
            this.areaKey = areaKey;
        }
    }
}
