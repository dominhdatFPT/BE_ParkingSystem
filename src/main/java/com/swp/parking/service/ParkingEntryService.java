package com.swp.parking.service;

import com.swp.parking.dto.request.ParkingEntryCheckRequest;
import com.swp.parking.dto.request.ParkingEntryConfirmRequest;
import com.swp.parking.dto.response.ParkingEntryResponse;
import com.swp.parking.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingEntryService {

    private static final List<String> ACTIVE_SUBSCRIPTION_STATUSES =
            List.of("ACTIVE", "PAID", "CONFIRMED", "APPROVED");

    private final JdbcTemplate jdbcTemplate;

    private volatile boolean initialized;

    @Transactional
    public ParkingEntryResponse checkVehicle(ParkingEntryCheckRequest request) {
        ensureOperationalTables();

        String licensePlate = normalizePlate(request.getLicensePlate());
        String vehicleType = normalizeVehicleType(request.getVehicleType(), licensePlate);

        if (licensePlate.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Nhap bien so xe de kiem tra");
        }

        if (hasActiveOrder(licensePlate)) {
            return invalid(licensePlate, vehicleType, "Xe nay dang co phien gui xe ACTIVE");
        }

        Optional<MonthlyVehicle> monthlyVehicle = findActiveMonthlyVehicle(licensePlate);
        if (monthlyVehicle.isPresent()) {
            MonthlyVehicle vehicle = monthlyVehicle.get();
            return ParkingEntryResponse.builder()
                    .entryType("MONTHLY")
                    .registered(true)
                    .hasActiveSubscription(true)
                    .vehicleId(vehicle.getVehicleId())
                    .customerName(vehicle.getCustomerName())
                    .vehicleBrand(vehicle.getBrand())
                    .vehicleColor(vehicle.getColor())
                    .licensePlate(vehicle.getLicensePlate())
                    .vehicleType(displayVehicleType(firstNonBlank(vehicle.getVehicleType(), vehicleType)))
                    .monthlyPackageName(vehicle.getPackageName())
                    .subscriptionEndDate(vehicle.getEndDate())
                    .entryTime(LocalDateTime.now())
                    .sessionStatus("Da tim thay goi thang - cho xac nhan xe vao")
                    .canConfirm(true)
                    .message("Xe da co goi thang, khong can cap the vang lai")
                    .build();
        }

        String nextCard = findFirstAvailableVisitorCard()
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Da het 100 the vang lai kha dung"))
                .getCardCode();

        return visitor(licensePlate, vehicleType, nextCard, true,
                "Xe chua co goi thang. Se cap the vang lai " + nextCard);
    }

    @Transactional
    public ParkingEntryResponse confirmEntry(ParkingEntryConfirmRequest request, Long staffUserId) {
        ensureOperationalTables();

        String licensePlate = normalizePlate(request.getLicensePlate());
        String requestedVisitorCard = normalizeCode(request.getVisitorCardCode());
        String vehicleType = normalizeVehicleType(request.getVehicleType(), licensePlate);

        if (licensePlate.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Can nhap bien so de tao phien xe vao");
        }

        Optional<MonthlyVehicle> monthlyVehicle = findActiveMonthlyVehicle(licensePlate);
        if (monthlyVehicle.isPresent()) {
            MonthlyVehicle vehicle = monthlyVehicle.get();
            licensePlate = firstNonBlank(licensePlate, vehicle.getLicensePlate());

            if (hasActiveOrder(licensePlate)) {
                throw new AppException(HttpStatus.CONFLICT, "Xe nay dang co phien gui xe ACTIVE");
            }

            Long orderId = insertParkingOrder(
                    licensePlate,
                    vehicle.getVehicleId(),
                    vehicle.getVehicleTypeId(),
                    vehicle.getSubscriptionId(),
                    null,
                    staffUserId,
                    "SUBSCRIPTION",
                    "ENTRY_TYPE=MONTHLY;PACKAGE=" + safeNote(vehicle.getPackageName())
            );

            return ParkingEntryResponse.builder()
                    .entryType("MONTHLY")
                    .registered(true)
                    .hasActiveSubscription(true)
                    .vehicleId(vehicle.getVehicleId())
                    .customerName(vehicle.getCustomerName())
                    .vehicleBrand(vehicle.getBrand())
                    .vehicleColor(vehicle.getColor())
                    .orderId(orderId)
                    .licensePlate(vehicle.getLicensePlate())
                    .vehicleType(displayVehicleType(firstNonBlank(vehicle.getVehicleType(), vehicleType)))
                    .monthlyPackageName(vehicle.getPackageName())
                    .subscriptionEndDate(vehicle.getEndDate())
                    .entryTime(LocalDateTime.now())
                    .sessionStatus("Da tao phien gui xe")
                    .canConfirm(false)
                    .message("Da xac nhan xe goi thang vao bai")
                    .build();
        }

        if (hasActiveOrder(licensePlate)) {
            throw new AppException(HttpStatus.CONFLICT, "Xe nay dang co phien gui xe ACTIVE");
        }

        VisitorCard visitorCard = lockAvailableVisitorCard(requestedVisitorCard)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "The vang lai khong kha dung hoac da het the"));

        Long vehicleTypeId = findVehicleTypeId(vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Chua cau hinh loai xe " + vehicleType));

        Long orderId = insertParkingOrder(
                licensePlate,
                null,
                vehicleTypeId,
                null,
                visitorCard.getId(),
                staffUserId,
                "VISITOR",
                "ENTRY_TYPE=VISITOR;VISITOR_CARD=" + visitorCard.getCardCode() + ";VEHICLE_TYPE=" + safeNote(vehicleType)
        );

        jdbcTemplate.update("""
                UPDATE visitor_cards
                SET status = 'IN_USE',
                    current_order_id = ?,
                    updated_at = now()
                WHERE visitor_card_id = ?
                """, orderId, visitorCard.getId());

        return ParkingEntryResponse.builder()
                .entryType("VISITOR")
                .registered(false)
                .hasActiveSubscription(false)
                .orderId(orderId)
                .licensePlate(licensePlate)
                .vehicleType(displayVehicleType(vehicleType))
                .visitorCardCode(visitorCard.getCardCode())
                .entryTime(LocalDateTime.now())
                .sessionStatus("Da tao phien gui xe")
                .canConfirm(false)
                .message("Da cap the vang lai " + visitorCard.getCardCode() + " cho xe vao bai")
                .build();
    }

    private ParkingEntryResponse visitor(String licensePlate, String vehicleType, String cardCode, boolean canConfirm, String message) {
        return ParkingEntryResponse.builder()
                .entryType("VISITOR")
                .registered(false)
                .hasActiveSubscription(false)
                .licensePlate(licensePlate)
                .vehicleType(displayVehicleType(vehicleType))
                .visitorCardCode(cardCode)
                .entryTime(LocalDateTime.now())
                .sessionStatus(canConfirm ? "Chua tao phien - cho xac nhan" : "Can bo sung bien so")
                .canConfirm(canConfirm)
                .message(message)
                .build();
    }

    private ParkingEntryResponse invalid(String licensePlate, String vehicleType, String message) {
        return ParkingEntryResponse.builder()
                .entryType("INVALID")
                .registered(false)
                .hasActiveSubscription(false)
                .licensePlate(licensePlate)
                .vehicleType(displayVehicleType(vehicleType))
                .sessionStatus("Khong hop le")
                .canConfirm(false)
                .message(message)
                .build();
    }

    private Optional<MonthlyVehicle> findActiveMonthlyVehicle(String licensePlate) {
        if (licensePlate.isBlank()) {
            return Optional.empty();
        }

        try {
            return jdbcTemplate.query("""
                    SELECT v.vehicle_id,
                           v.license_plate,
                           v.brand,
                           v.color,
                           u.full_name AS customer_name,
                           vt.vehicle_type_id,
                           vt.type_name AS vehicle_type,
                           fs.fee_subscription_id,
                           fp.name AS package_name,
                           fs.end_date
                    FROM vehicles v
                    JOIN customers c ON c.customer_id = v.customer_id
                    JOIN users u ON u.user_id = c.user_id
                    JOIN fee_subscription fs ON fs.vehicle_id = v.vehicle_id
                    JOIN fee_package fp ON fp.fee_package_id = fs.fee_package_id
                    LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
                    WHERE regexp_replace(upper(v.license_plate), '[^A-Z0-9]', '', 'g') = ?
                      AND upper(fs.status) IN (?, ?, ?, ?)
                      AND (fs.end_date IS NULL OR fs.end_date >= now())
                    ORDER BY fs.end_date DESC NULLS FIRST
                    LIMIT 1
                    """,
                    (rs, rowNum) -> MonthlyVehicle.builder()
                            .vehicleId(rs.getLong("vehicle_id"))
                            .vehicleTypeId(rs.getLong("vehicle_type_id"))
                            .subscriptionId(rs.getLong("fee_subscription_id"))
                            .licensePlate(rs.getString("license_plate"))
                            .customerName(rs.getString("customer_name"))
                            .brand(rs.getString("brand"))
                            .color(rs.getString("color"))
                            .vehicleType(rs.getString("vehicle_type"))
                            .packageName(rs.getString("package_name"))
                            .endDate(rs.getTimestamp("end_date") == null
                                    ? null
                                    : rs.getTimestamp("end_date").toLocalDateTime())
                            .build(),
                    compactPlate(licensePlate),
                    ACTIVE_SUBSCRIPTION_STATUSES.get(0),
                    ACTIVE_SUBSCRIPTION_STATUSES.get(1),
                    ACTIVE_SUBSCRIPTION_STATUSES.get(2),
                    ACTIVE_SUBSCRIPTION_STATUSES.get(3)
            ).stream().findFirst();
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private Optional<Long> findVehicleTypeId(String vehicleType) {
        String expectedCode = "MOTORBIKE".equals(vehicleType) ? "MOTORBIKE" : "CAR";
        return jdbcTemplate.query("""
                SELECT vehicle_type_id
                FROM vehicle_types
                WHERE upper(COALESCE(type_code, '')) = ?
                   OR (? = 'MOTORBIKE' AND upper(type_name) LIKE '%MOTOR%')
                   OR (? = 'CAR' AND upper(type_name) NOT LIKE '%MOTOR%')
                ORDER BY CASE WHEN upper(COALESCE(type_code, '')) = ? THEN 0 ELSE 1 END
                LIMIT 1
                """, (rs, rowNum) -> rs.getLong("vehicle_type_id"),
                expectedCode, expectedCode, expectedCode, expectedCode).stream().findFirst();
    }

    private boolean hasActiveOrder(String licensePlate) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT count(*)
                FROM parking_orders
                WHERE regexp_replace(upper(license_plate), '[^A-Z0-9]', '', 'g') = ?
                  AND parking_status = 'ACTIVE'
                """, Integer.class, compactPlate(licensePlate));
        return count != null && count > 0;
    }

    private Optional<VisitorCard> findFirstAvailableVisitorCard() {
        try {
            return jdbcTemplate.query("""
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                    ORDER BY display_number
                    LIMIT 1
                    """, (rs, rowNum) -> VisitorCard.builder()
                            .id(rs.getLong("visitor_card_id"))
                            .cardCode(rs.getString("card_code"))
                            .displayNumber(rs.getInt("display_number"))
                            .status(rs.getString("status"))
                            .build()
            ).stream().findFirst();
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<VisitorCard> lockAvailableVisitorCard(String cardCode) {
        String sql;
        Object[] args;

        if (cardCode == null || cardCode.isBlank()) {
            sql = """
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                    ORDER BY display_number
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                    """;
            args = new Object[]{};
        } else {
            sql = """
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE card_code = ?
                      AND status = 'AVAILABLE'
                    LIMIT 1
                    FOR UPDATE
                    """;
            args = new Object[]{cardCode};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> VisitorCard.builder()
                .id(rs.getLong("visitor_card_id"))
                .cardCode(rs.getString("card_code"))
                .displayNumber(rs.getInt("display_number"))
                .status(rs.getString("status"))
                .build(), args
        ).stream().findFirst();
    }

    private Long insertParkingOrder(
            String licensePlate,
            Long vehicleId,
            Long vehicleTypeId,
            Long subscriptionId,
            Long visitorCardId,
            Long staffUserId,
            String entryType,
            String notes) {
        String orderCode = "PO-" + System.currentTimeMillis();
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO parking_orders (
                        order_code,
                        vehicle_id,
                        vehicle_type_id,
                        subscription_id,
                        visitor_card_id,
                        license_plate,
                        entry_time,
                        parking_status,
                        entry_type,
                        payment_status,
                        checked_in_by,
                        notes,
                        created_at,
                        updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, now(), 'ACTIVE', ?, ?, ?, ?, now(), now())
                    """, new String[]{"order_id"});
            statement.setString(1, orderCode);
            if (vehicleId == null) {
                statement.setObject(2, null);
            } else {
                statement.setLong(2, vehicleId);
            }
            if (vehicleTypeId == null) {
                statement.setObject(3, null);
            } else {
                statement.setLong(3, vehicleTypeId);
            }
            if (subscriptionId == null) {
                statement.setObject(4, null);
            } else {
                statement.setLong(4, subscriptionId);
            }
            if (visitorCardId == null) {
                statement.setObject(5, null);
            } else {
                statement.setLong(5, visitorCardId);
            }
            statement.setString(6, licensePlate);
            statement.setString(7, entryType);
            statement.setString(8, "SUBSCRIPTION".equals(entryType) ? "NOT_REQUIRED" : "UNPAID");
            statement.setLong(9, staffUserId);
            statement.setString(10, notes);
            return statement;
        }, keyHolder);

        Number generatedId = keyHolder.getKey();
        if (generatedId != null) {
            return generatedId.longValue();
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT order_id FROM parking_orders WHERE order_code = ?",
                orderCode
        );
        return ((Number) rows.get(0).get("order_id")).longValue();
    }

    private void ensureOperationalTables() {
        if (initialized) {
            return;
        }

        synchronized (this) {
            if (initialized) {
                return;
            }

            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS parking_orders (
                        order_id bigserial PRIMARY KEY,
                        order_code varchar(64) UNIQUE,
                        parking_id bigint,
                        floor_id bigint,
                        vehicle_id bigint,
                        license_plate varchar(50),
                        entry_gate_id bigint,
                        card_id bigint,
                        entry_time timestamp,
                        exit_time timestamp,
                        calculated_fee numeric(19, 2),
                        parking_status varchar(30),
                        checked_in_by bigint,
                        notes text,
                        created_at timestamp NOT NULL DEFAULT now(),
                        updated_at timestamp NOT NULL DEFAULT now()
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_parking_orders_plate_status
                    ON parking_orders (license_plate, parking_status)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS visitor_cards (
                        visitor_card_id bigserial PRIMARY KEY,
                        card_code varchar(20) NOT NULL UNIQUE,
                        display_number integer NOT NULL UNIQUE,
                        status varchar(30) NOT NULL DEFAULT 'AVAILABLE',
                        current_order_id bigint,
                        created_at timestamp NOT NULL DEFAULT now(),
                        updated_at timestamp NOT NULL DEFAULT now()
                    )
                    """);
            jdbcTemplate.execute("""
                    INSERT INTO visitor_cards (card_code, display_number, status, created_at, updated_at)
                    SELECT concat('VIS', lpad(n::text, 3, '0')), n, 'AVAILABLE', now(), now()
                    FROM generate_series(1, 100) AS n
                    ON CONFLICT (card_code) DO NOTHING
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_visitor_cards_status_number
                    ON visitor_cards (status, display_number)
                    """);

            initialized = true;
        }
    }

    private String normalizePlate(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVehicleType(String value, String licensePlate) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if ("MOTORBIKE".equals(normalized) || normalized.contains("XE MÁY")) {
            return "MOTORBIKE";
        }
        if ("CAR".equals(normalized) || normalized.contains("Ô TÔ")) {
            return "CAR";
        }
        String plate = normalizePlate(licensePlate).replace(" ", "");
        return plate.matches("^\\d{2}[A-Z]\\d[-.].*") ? "MOTORBIKE" : "CAR";
    }

    private String displayVehicleType(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (normalized.contains("MOTORBIKE") || normalized.contains("XE MÁY")) {
            return "Xe máy";
        }
        return "Ô tô";
    }

    private String compactPlate(String value) {
        return normalizePlate(value).replaceAll("[^A-Z0-9]", "");
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return Objects.requireNonNullElse(second, "");
    }

    private String safeNote(String value) {
        return value == null ? "" : value.replace(";", ",");
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class VisitorCard {
        private Long id;
        private String cardCode;
        private Integer displayNumber;
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class MonthlyVehicle {
        private Long vehicleId;
        private Long vehicleTypeId;
        private Long subscriptionId;
        private String licensePlate;
        private String customerName;
        private String brand;
        private String color;
        private String vehicleType;
        private String packageName;
        private LocalDateTime endDate;
    }
}
