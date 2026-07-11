package com.swp.parking.service;

import com.swp.parking.dto.response.OperationsDashboardResponse;
import com.swp.parking.dto.response.ParkingOperationsResponse;
import com.swp.parking.model.enums.ParkingSlotStatus;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    public OperationsDashboardResponse getDashboard(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        List<ParkingOperationsResponse.Slot> slots = readOperationSlots();
        DashboardMetricsData dashboardMetrics = readDashboardMetrics(targetDate);
        long totalSlots = Math.max(slots.size(), dashboardMetrics.visitorCards());
        long availableSlots = slots.isEmpty()
                ? dashboardMetrics.availableVisitorCards()
                : slots.stream().filter(slot -> slot.getStatus() == ParkingSlotStatus.AVAILABLE).count();
        long vehiclesInParking = dashboardMetrics.vehiclesInParking();
        if (vehiclesInParking == 0) {
            vehiclesInParking = slots.stream()
                    .filter(slot -> slot.getStatus() == ParkingSlotStatus.OCCUPIED || slot.getStatus() == ParkingSlotStatus.RESERVED)
                    .count();
        }

        OperationsDashboardResponse.Metrics metrics = OperationsDashboardResponse.Metrics.builder()
                .vehiclesInParking(vehiclesInParking)
                .availableSlots(availableSlots)
                .vehiclesInToday(dashboardMetrics.vehiclesInToday())
                .vehiclesInTodayCars(dashboardMetrics.vehiclesInTodayCars())
                .vehiclesInTodayMotorbikes(dashboardMetrics.vehiclesInTodayMotorbikes())
                .vehiclesOutToday(dashboardMetrics.vehiclesOutToday())
                .vehiclesOutTodayCars(dashboardMetrics.vehiclesOutTodayCars())
                .vehiclesOutTodayMotorbikes(dashboardMetrics.vehiclesOutTodayMotorbikes())
                .activeCars(dashboardMetrics.activeCars())
                .activeMotorbikes(dashboardMetrics.activeMotorbikes())
                .openIncidents(dashboardMetrics.openIncidents())
                .revenueToday(dashboardMetrics.revenueToday())
                .totalSlots(totalSlots)
                .occupancyRate(totalSlots > 0 ? (int) Math.round((vehiclesInParking * 100.0) / totalSlots) : 0)
                .build();

        return OperationsDashboardResponse.builder()
                .metrics(metrics)
                .areaOccupancy(buildAreaOccupancy(slots))
                .trafficByHour(readTrafficByHour(targetDate))
                .recentIncidents(readRecentIncidents())
                .recentVehicleActivities(readRecentVehicleActivities())
                .build();
    }

    private DashboardMetricsData readDashboardMetrics(LocalDate date) {
        String sql = """
                WITH params AS (
                    SELECT ?::date AS start_date, ?::date AS end_date
                ),
                order_metrics AS (
                    SELECT
                        COUNT(*) FILTER (
                            WHERE po.parking_status = 'ACTIVE'
                               OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL)
                        ) AS vehicles_in_parking,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                        ) AS vehicles_in_today,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                              AND %1$s = 'CAR'
                        ) AS vehicles_in_today_cars,
                        COUNT(*) FILTER (
                            WHERE po.entry_time >= p.start_date AND po.entry_time < p.end_date
                              AND %1$s = 'MOTORBIKE'
                        ) AS vehicles_in_today_motorbikes,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                        ) AS vehicles_out_today,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                              AND %1$s = 'CAR'
                        ) AS vehicles_out_today_cars,
                        COUNT(*) FILTER (
                            WHERE po.exit_time >= p.start_date AND po.exit_time < p.end_date
                              AND %1$s = 'MOTORBIKE'
                        ) AS vehicles_out_today_motorbikes,
                        COUNT(*) FILTER (
                            WHERE (po.parking_status = 'ACTIVE'
                                OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))
                              AND %1$s = 'CAR'
                        ) AS active_cars,
                        COUNT(*) FILTER (
                            WHERE (po.parking_status = 'ACTIVE'
                                OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))
                              AND %1$s = 'MOTORBIKE'
                        ) AS active_motorbikes,
                        COUNT(*) FILTER (
                            WHERE COALESCE(po.notes, '') <> ''
                              AND COALESCE(po.parking_status, '') IN ('ISSUE', 'EXCEPTION', 'OPEN', 'ACTIVE')
                        ) AS open_incidents,
                        COALESCE(SUM(po.calculated_fee) FILTER (
                            WHERE po.calculated_fee IS NOT NULL
                              AND po.exit_time >= p.start_date AND po.exit_time < p.end_date
                        ), 0) AS revenue_today
                    FROM parking_orders po
                    CROSS JOIN params p
                    LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                    LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                ),
                visitor_card_metrics AS (
                    SELECT COUNT(*) AS visitor_cards,
                           COUNT(*) FILTER (WHERE status = 'AVAILABLE') AS available_visitor_cards
                    FROM visitor_cards
                )
                SELECT *
                FROM order_metrics
                CROSS JOIN visitor_card_metrics
                """.formatted(vehicleTypeExpression());

        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new DashboardMetricsData(
                    rs.getLong("vehicles_in_parking"),
                    rs.getLong("vehicles_in_today"),
                    rs.getLong("vehicles_in_today_cars"),
                    rs.getLong("vehicles_in_today_motorbikes"),
                    rs.getLong("vehicles_out_today"),
                    rs.getLong("vehicles_out_today_cars"),
                    rs.getLong("vehicles_out_today_motorbikes"),
                    rs.getLong("active_cars"),
                    rs.getLong("active_motorbikes"),
                    rs.getLong("open_incidents"),
                    rs.getBigDecimal("revenue_today"),
                    rs.getLong("visitor_cards"),
                    rs.getLong("available_visitor_cards")
            ), date, date.plusDays(1));
        } catch (DataAccessException ex) {
            log.warn("Could not read dashboard metrics in one query, using fallback queries: {}", ex.getMessage());
            return new DashboardMetricsData(
                    countActiveParkingOrders(),
                    countVehiclesInToday(date),
                    countVehiclesInTodayByType(date, "CAR"),
                    countVehiclesInTodayByType(date, "MOTORBIKE"),
                    countVehiclesOutToday(date),
                    countVehiclesOutTodayByType(date, "CAR"),
                    countVehiclesOutTodayByType(date, "MOTORBIKE"),
                    countActiveVehiclesByType("CAR"),
                    countActiveVehiclesByType("MOTORBIKE"),
                    countOpenIncidents(),
                    sumRevenueToday(date),
                    countVisitorCards(),
                    countAvailableVisitorCards()
            );
        }
    }

    public List<OperationsDashboardResponse.VehicleActivity> getParkingSessions(
            String search,
            String tab,
            String vehicleType,
            String customerType,
            String status,
            LocalDate date,
            int page,
            int size
    ) {
        return readVehicleActivities(ParkingSessionQuery.builder()
                .search(search)
                .tab(tab)
                .vehicleType(vehicleType)
                .customerType(customerType)
                .status(status)
                .date(date)
                .page(page)
                .size(size)
                .build());
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
                LEFT JOIN first_floor ff ON ff.floor_number = ps.floor
                LEFT JOIN parking_floors fl ON fl.floor_id = ff.floor_id
                LEFT JOIN parking_facilities pf ON pf.parking_id = fl.parking_id
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

    private long countActiveParkingOrders() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE parking_status = 'ACTIVE'
                   OR (entry_time IS NOT NULL AND exit_time IS NULL)
                """);
    }

    private long countVehiclesInToday(LocalDate date) {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE entry_time >= ?
                  AND entry_time < ?
                """, date, date.plusDays(1));
    }

    private long countVehiclesInTodayByType(LocalDate date, String vehicleType) {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders po
                LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                WHERE po.entry_time >= ?
                  AND po.entry_time < ?
                  AND %s = ?
                """.formatted(vehicleTypeExpression()), date, date.plusDays(1), vehicleType);
    }

    private long countVehiclesOutToday(LocalDate date) {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE exit_time >= ?
                  AND exit_time < ?
                """, date, date.plusDays(1));
    }

    private long countVehiclesOutTodayByType(LocalDate date, String vehicleType) {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders po
                LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                WHERE po.exit_time >= ?
                  AND po.exit_time < ?
                  AND %s = ?
                """.formatted(vehicleTypeExpression()), date, date.plusDays(1), vehicleType);
    }

    private long countActiveVehiclesByType(String vehicleType) {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders po
                LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                WHERE (po.parking_status = 'ACTIVE' OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))
                  AND %s = ?
                """.formatted(vehicleTypeExpression()), vehicleType);
    }

    private long countVisitorCards() {
        return queryLong("SELECT COUNT(*) FROM visitor_cards");
    }

    private long countAvailableVisitorCards() {
        return queryLong("SELECT COUNT(*) FROM visitor_cards WHERE status = 'AVAILABLE'");
    }

    private long countOpenIncidents() {
        return queryLong("""
                SELECT COUNT(*)
                FROM parking_orders
                WHERE COALESCE(notes, '') <> ''
                  AND COALESCE(parking_status, '') IN ('ISSUE', 'EXCEPTION', 'OPEN', 'ACTIVE')
                """);
    }

    private BigDecimal sumRevenueToday(LocalDate date) {
        String sql = """
                SELECT COALESCE(SUM(calculated_fee), 0)
                FROM parking_orders
                WHERE calculated_fee IS NOT NULL
                  AND exit_time >= ?
                  AND exit_time < ?
                """;
        try {
            BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class, date, date.plusDays(1));
            return value != null ? value : BigDecimal.ZERO;
        } catch (DataAccessException ex) {
            log.warn("Could not calculate today revenue: {}", ex.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private List<OperationsDashboardResponse.TrafficPoint> readTrafficByHour(LocalDate date) {
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
                WHERE entry_time >= ?
                  AND entry_time < ?
                GROUP BY bucket
                """, date, date.plusDays(1));
        mergeTraffic(points, "out", """
                SELECT FLOOR(EXTRACT(HOUR FROM exit_time) / 2)::int AS bucket, COUNT(*) AS count
                FROM parking_orders
                WHERE exit_time >= ?
                  AND exit_time < ?
                GROUP BY bucket
                """, date, date.plusDays(1));

        return points;
    }

    private void mergeTraffic(List<OperationsDashboardResponse.TrafficPoint> points, String type, String sql, Object... args) {
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
            }, args);
        } catch (DataAccessException ex) {
            log.warn("Could not read traffic {} by hour: {}", type, ex.getMessage());
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
        return readVehicleActivities(ParkingSessionQuery.builder().size(8).build());
    }

    private List<OperationsDashboardResponse.VehicleActivity> readVehicleActivities(ParkingSessionQuery query) {
        ParkingSessionQuery safeQuery = query.safe();
        String vehicleTypeExpression = vehicleTypeExpression();
        String customerTypeExpression = customerTypeExpression();
        StringBuilder where = new StringBuilder();
        List<Object> args = new ArrayList<>();

        addParkingSessionFilters(where, args, safeQuery, vehicleTypeExpression, customerTypeExpression);
        args.add(safeQuery.size());
        args.add((long) safeQuery.page() * safeQuery.size());

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
                    %1$s AS vehicle_type,
                    %2$s AS customer_type,
                    COALESCE(vc_by_id.card_code, vc_by_order.card_code, substring(COALESCE(po.notes, '') from 'VISITOR_CARD=([^;]+)')) AS visitor_card_code,
                    pf.parking_name,
                    fl.floor_name
                FROM parking_orders po
                LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = COALESCE(po.vehicle_type_id, v.vehicle_type_id)
                LEFT JOIN visitor_cards vc_by_id ON vc_by_id.visitor_card_id = COALESCE(po.visitor_card_id, po.card_id)
                LEFT JOIN visitor_cards vc_by_order ON po.visitor_card_id IS NULL AND vc_by_order.current_order_id = po.order_id
                LEFT JOIN parking_facilities pf ON pf.parking_id = po.parking_id
                LEFT JOIN parking_floors fl ON fl.floor_id = po.floor_id
                %3$s
                ORDER BY po.updated_at DESC
                LIMIT ? OFFSET ?
                """.formatted(
                vehicleTypeExpression,
                customerTypeExpression,
                where
        );
        try {
            return jdbcTemplate.query(sql, this::mapVehicleActivity, args.toArray());
        } catch (DataAccessException ex) {
            log.warn("Could not read parking sessions: {}", ex.getMessage());
            return List.of();
        }
    }

    private void addParkingSessionFilters(
            StringBuilder where,
            List<Object> args,
            ParkingSessionQuery query,
            String vehicleTypeExpression,
            String customerTypeExpression
    ) {
        List<String> clauses = new ArrayList<>();
        String search = normalizeFilter(query.search());
        if (!search.isBlank()) {
            clauses.add("upper(po.license_plate) LIKE ?");
            args.add("%" + search + "%");
        }

        String tab = normalizeFilter(query.tab());
        if ("ACTIVE".equals(tab)) {
            clauses.add(activeParkingOrderCondition());
        } else if ("COMPLETED".equals(tab)) {
            clauses.add(completedParkingOrderCondition());
        }

        String vehicleType = normalizeFilter(query.vehicleType());
        if ("CAR".equals(vehicleType) || "MOTORBIKE".equals(vehicleType)) {
            clauses.add(vehicleTypeExpression + " = ?");
            args.add(vehicleType);
        }

        String customerType = normalizeFilter(query.customerType());
        if ("MONTHLY".equals(customerType) || "VISITOR".equals(customerType)) {
            clauses.add(customerTypeExpression + " = ?");
            args.add(customerType);
        }

        String status = normalizeFilter(query.status());
        switch (status) {
            case "COMPLETED" -> clauses.add(completedParkingOrderCondition());
            case "NORMAL" -> clauses.add(activeParkingOrderCondition() + " AND po.entry_time > now() - interval '24 hours'");
            case "OVER_24_HOURS" -> clauses.add(activeParkingOrderCondition() + " AND po.entry_time <= now() - interval '24 hours' AND po.entry_time > now() - interval '7 days'");
            case "OVER_7_DAYS" -> clauses.add(activeParkingOrderCondition() + " AND po.entry_time <= now() - interval '7 days'");
            default -> {
            }
        }

        if (query.date() != null) {
            clauses.add("po.entry_time >= ? AND po.entry_time < ?");
            args.add(query.date());
            args.add(query.date().plusDays(1));
        }

        if (!clauses.isEmpty()) {
            where.append("WHERE ").append(String.join(" AND ", clauses));
        }
    }

    private OperationsDashboardResponse.VehicleActivity mapVehicleActivity(ResultSet rs, int rowNum) throws SQLException {
        return OperationsDashboardResponse.VehicleActivity.builder()
                .id(getLong(rs, "order_id"))
                .orderCode(rs.getString("order_code"))
                .licensePlate(rs.getString("license_plate"))
                .parkingName(rs.getString("parking_name"))
                .floorName(rs.getString("floor_name"))
                .vehicleType(rs.getString("vehicle_type"))
                .customerType(rs.getString("customer_type"))
                .visitorCardCode(rs.getString("visitor_card_code"))
                .status(rs.getString("parking_status"))
                .entryTime(getLocalDateTime(rs, "entry_time"))
                .exitTime(getLocalDateTime(rs, "exit_time"))
                .calculatedFee(rs.getBigDecimal("calculated_fee"))
                .updatedAt(getLocalDateTime(rs, "updated_at"))
                .build();
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

    private long queryLong(String sql, Object... args) {
        try {
            Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
            return value != null ? value : 0;
        } catch (DataAccessException ex) {
            log.warn("Could not read dashboard metric: {}", ex.getMessage());
            return 0;
        }
    }

    private String vehicleTypeExpression() {
        return """
                CASE
                    WHEN upper(COALESCE(po.notes, '')) LIKE '%%VEHICLE_TYPE=CAR%%'
                    THEN 'CAR'
                    WHEN upper(COALESCE(po.notes, '')) LIKE '%%VEHICLE_TYPE=MOTORBIKE%%'
                    THEN 'MOTORBIKE'
                    WHEN upper(COALESCE(vt.type_code, '')) LIKE 'CAR%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%CAR%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%O TO%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%OTO%%'
                    THEN 'CAR'
                    WHEN upper(COALESCE(vt.type_code, '')) LIKE 'MOTOR%%'
                      OR upper(COALESCE(vt.type_code, '')) LIKE 'MOTO%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%MOTOR%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%MOTO%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%BIKE%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%XE MAY%%'
                      OR upper(COALESCE(vt.type_name, '')) LIKE '%%XE M_Y%%'
                    THEN 'MOTORBIKE'
                    ELSE 'CAR'
                END
                """;
    }

    private String customerTypeExpression() {
        return """
                CASE
                    WHEN upper(COALESCE(po.entry_type, '')) IN ('MONTHLY', 'SUBSCRIPTION')
                      OR po.subscription_id IS NOT NULL
                      OR upper(COALESCE(po.notes, '')) LIKE 'ENTRY_TYPE=MONTHLY%%'
                    THEN 'MONTHLY'
                    ELSE 'VISITOR'
                END
                """;
    }

    private String activeParkingOrderCondition() {
        return "(po.parking_status = 'ACTIVE' OR (po.entry_time IS NOT NULL AND po.exit_time IS NULL))";
    }

    private String completedParkingOrderCondition() {
        return "(po.parking_status = 'COMPLETED' OR po.exit_time IS NOT NULL)";
    }

    private String normalizeFilter(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
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

    @Builder
    private record ParkingSessionQuery(
            String search,
            String tab,
            String vehicleType,
            String customerType,
            String status,
            LocalDate date,
            int page,
            int size
    ) {
        private ParkingSessionQuery safe() {
            int safePage = Math.max(page, 0);
            int safeSize = size <= 0 ? 100 : Math.min(size, 200);
            return new ParkingSessionQuery(search, tab, vehicleType, customerType, status, date, safePage, safeSize);
        }
    }

    private record DashboardMetricsData(
            long vehiclesInParking,
            long vehiclesInToday,
            long vehiclesInTodayCars,
            long vehiclesInTodayMotorbikes,
            long vehiclesOutToday,
            long vehiclesOutTodayCars,
            long vehiclesOutTodayMotorbikes,
            long activeCars,
            long activeMotorbikes,
            long openIncidents,
            BigDecimal revenueToday,
            long visitorCards,
            long availableVisitorCards) {
    }
}
