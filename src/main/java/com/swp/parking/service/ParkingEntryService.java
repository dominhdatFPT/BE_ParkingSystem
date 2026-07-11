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
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class ParkingEntryService {

    private static final List<String> ACTIVE_SUBSCRIPTION_STATUSES =
            List.of("ACTIVE", "PAID", "CONFIRMED", "APPROVED");
    private static final int VISITOR_CARDS_PER_VEHICLE_TYPE = 100;

    private final JdbcTemplate jdbcTemplate;

    private volatile boolean initialized;
    private final AtomicInteger orderCodeSequence = new AtomicInteger();

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

        String nextCard = findFirstAvailableVisitorCard(vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT,
                        "Da het 100 the vang lai kha dung cho " + displayVehicleType(vehicleType)))
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

            ParkingOrderRef order = insertParkingOrder(
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
                    .orderId(order.getId())
                    .orderCode(order.getCode())
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

        VisitorCard visitorCard = lockAvailableVisitorCard(requestedVisitorCard, vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT,
                        "The vang lai khong kha dung, da het the, hoac khong dung loai " + displayVehicleType(vehicleType)));

        Long vehicleTypeId = findVehicleTypeId(vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Chua cau hinh loai xe " + vehicleType));

        ParkingOrderRef order = insertParkingOrder(
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
                """, order.getId(), visitorCard.getId());

        return ParkingEntryResponse.builder()
                .entryType("VISITOR")
                .registered(false)
                .hasActiveSubscription(false)
                .orderId(order.getId())
                .orderCode(order.getCode())
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
        List<VehicleTypeRef> vehicleTypes = jdbcTemplate.query("""
                SELECT vehicle_type_id, type_code, type_name
                FROM vehicle_types
                ORDER BY vehicle_type_id
                """, (rs, rowNum) -> new VehicleTypeRef(
                rs.getLong("vehicle_type_id"),
                rs.getString("type_code"),
                rs.getString("type_name")
        ));

        return vehicleTypes.stream()
                .filter(type -> expectedCode.equals(resolveVehicleTypeToken(type.typeCode())))
                .map(VehicleTypeRef::id)
                .findFirst()
                .or(() -> vehicleTypes.stream()
                        .filter(type -> expectedCode.equals(resolveVehicleTypeToken(type.typeName())))
                        .map(VehicleTypeRef::id)
                        .findFirst());
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

    private Optional<VisitorCard> findFirstAvailableVisitorCard(String vehicleType) {
        Long vehicleTypeId = findVehicleTypeId(vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Chua cau hinh loai xe " + vehicleType));
        try {
            return jdbcTemplate.query("""
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                      AND vehicle_type_id = ?
                    ORDER BY display_number
                    LIMIT 1
                    """, (rs, rowNum) -> VisitorCard.builder()
                            .id(rs.getLong("visitor_card_id"))
                            .cardCode(rs.getString("card_code"))
                            .displayNumber(rs.getInt("display_number"))
                            .status(rs.getString("status"))
                            .build(),
                    vehicleTypeId
            ).stream().findFirst();
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    private Optional<VisitorCard> lockAvailableVisitorCard(String cardCode, String vehicleType) {
        Long vehicleTypeId = findVehicleTypeId(vehicleType)
                .orElseThrow(() -> new AppException(HttpStatus.CONFLICT, "Chua cau hinh loai xe " + vehicleType));
        String sql;
        Object[] args;

        if (cardCode == null || cardCode.isBlank()) {
            sql = """
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE status = 'AVAILABLE'
                      AND vehicle_type_id = ?
                    ORDER BY display_number
                    LIMIT 1
                    FOR UPDATE SKIP LOCKED
                    """;
            args = new Object[]{vehicleTypeId};
        } else {
            sql = """
                    SELECT visitor_card_id, card_code, display_number, status
                    FROM visitor_cards
                    WHERE card_code = ?
                      AND vehicle_type_id = ?
                      AND status = 'AVAILABLE'
                    LIMIT 1
                    FOR UPDATE
                    """;
            args = new Object[]{cardCode, vehicleTypeId};
        }

        return jdbcTemplate.query(sql, (rs, rowNum) -> VisitorCard.builder()
                .id(rs.getLong("visitor_card_id"))
                .cardCode(rs.getString("card_code"))
                .displayNumber(rs.getInt("display_number"))
                .status(rs.getString("status"))
                .build(), args
        ).stream().findFirst();
    }

    private ParkingOrderRef insertParkingOrder(
            String licensePlate,
            Long vehicleId,
            Long vehicleTypeId,
            Long subscriptionId,
            Long visitorCardId,
            Long staffUserId,
            String entryType,
            String notes) {
        String orderCode = generateOrderCode();
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
            return new ParkingOrderRef(generatedId.longValue(), orderCode);
        }

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT order_id FROM parking_orders WHERE order_code = ?",
                orderCode
        );
        return new ParkingOrderRef(((Number) rows.get(0).get("order_id")).longValue(), orderCode);
    }

    private String generateOrderCode() {
        String timestamp = Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        if (timestamp.length() > 8) {
            timestamp = timestamp.substring(timestamp.length() - 8);
        }
        int sequence = Math.floorMod(orderCodeSequence.getAndIncrement(), 36);
        String suffix = String.valueOf(Character.forDigit(sequence, 36)).toUpperCase(Locale.ROOT);
        return "P" + timestamp + suffix;
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
                        vehicle_type_id bigint,
                        status varchar(30) NOT NULL DEFAULT 'AVAILABLE',
                        current_order_id bigint,
                        created_at timestamp NOT NULL DEFAULT now(),
                        updated_at timestamp NOT NULL DEFAULT now()
                    )
                    """);
            jdbcTemplate.execute("""
                    ALTER TABLE visitor_cards
                    ADD COLUMN IF NOT EXISTS vehicle_type_id bigint
                    """);
            ensureVisitorCardPools();
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_visitor_cards_status_number
                    ON visitor_cards (status, display_number)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_visitor_cards_type_status_number
                    ON visitor_cards (vehicle_type_id, status, display_number)
                    """);

            initialized = true;
        }
    }

    private void ensureVisitorCardPools() {
        Optional<Long> motorbikeTypeId = findVehicleTypeId("MOTORBIKE");
        Optional<Long> carTypeId = findVehicleTypeId("CAR");
        if (motorbikeTypeId.isEmpty() || carTypeId.isEmpty()) {
            return;
        }

        jdbcTemplate.update("""
                UPDATE visitor_cards vc
                   SET vehicle_type_id = ?, updated_at = now()
                  FROM parking_orders po
                 WHERE vc.vehicle_type_id IS NULL
                   AND (vc.current_order_id = po.order_id OR po.visitor_card_id = vc.visitor_card_id)
                   AND upper(COALESCE(po.notes, '')) LIKE '%%VEHICLE_TYPE=CAR%%'
                """, carTypeId.get());
        jdbcTemplate.update("""
                UPDATE visitor_cards vc
                   SET vehicle_type_id = ?, updated_at = now()
                  FROM parking_orders po
                 WHERE vc.vehicle_type_id IS NULL
                   AND (vc.current_order_id = po.order_id OR po.visitor_card_id = vc.visitor_card_id)
                   AND upper(COALESCE(po.notes, '')) LIKE '%%VEHICLE_TYPE=MOTORBIKE%%'
                """, motorbikeTypeId.get());
        jdbcTemplate.update("""
                UPDATE visitor_cards
                   SET vehicle_type_id = ?, updated_at = now()
                 WHERE vehicle_type_id IS NULL
                """, motorbikeTypeId.get());

        ensureVisitorCardPool(motorbikeTypeId.get(), "MOTO", 1000);
        ensureVisitorCardPool(carTypeId.get(), "CAR", 2000);
    }

    private void ensureVisitorCardPool(Long vehicleTypeId, String codePrefix, int displayBase) {
        Integer currentCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM visitor_cards
                 WHERE vehicle_type_id = ?
                """, Integer.class, vehicleTypeId);
        int count = currentCount == null ? 0 : currentCount;
        for (int n = 1; count < VISITOR_CARDS_PER_VEHICLE_TYPE && n <= 300; n++) {
            String cardCode = codePrefix + String.format("%03d", n);
            int displayNumber = displayBase + n;
            int inserted = jdbcTemplate.update("""
                    INSERT INTO visitor_cards (card_code, display_number, vehicle_type_id, status, created_at, updated_at)
                    SELECT ?, ?, ?, 'AVAILABLE', now(), now()
                    WHERE NOT EXISTS (
                        SELECT 1
                          FROM visitor_cards
                         WHERE vehicle_type_id = ?
                           AND card_code = ?
                    )
                    ON CONFLICT DO NOTHING
                    """, cardCode, displayNumber, vehicleTypeId, vehicleTypeId, cardCode);
            count += inserted;
        }
    }

    private String normalizePlate(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVehicleType(String value, String licensePlate) {
        String resolved = resolveVehicleTypeToken(value);
        if (!resolved.isBlank()) {
            return resolved;
        }
        String plate = normalizePlate(licensePlate).replace(" ", "");
        return plate.matches("^\\d{2}[A-Z]\\d[-.].*") ? "MOTORBIKE" : "CAR";
    }

    private String displayVehicleType(String value) {
        if ("MOTORBIKE".equals(resolveVehicleTypeToken(value))) {
            return "Xe máy";
        }
        return "Ô tô";
    }

    private String resolveVehicleTypeToken(String value) {
        String normalized = normalizeSearchText(value);
        if (normalized.isBlank()) {
            return "";
        }
        if (normalized.equals("MOTORBIKE")
                || normalized.contains("MOTOR")
                || normalized.contains("MOTO")
                || normalized.contains("BIKE")
                || normalized.contains("XE MAY")) {
            return "MOTORBIKE";
        }
        if (normalized.equals("CAR")
                || normalized.contains("AUTO")
                || normalized.contains("O TO")
                || normalized.contains("OTO")) {
            return "CAR";
        }
        return "";
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }
        String text = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return text.replaceAll("[^A-Z0-9]+", " ").trim().replaceAll("\\s+", " ");
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
    private static class ParkingOrderRef {
        private Long id;
        private String code;
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

    private record VehicleTypeRef(Long id, String typeCode, String typeName) {
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
