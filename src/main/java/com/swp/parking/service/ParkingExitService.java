package com.swp.parking.service;

import com.swp.parking.dto.request.ParkingExitCheckRequest;
import com.swp.parking.dto.request.ParkingExitConfirmRequest;
import com.swp.parking.dto.response.ParkingExitResponse;
import com.swp.parking.exception.AppException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ParkingExitService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public ParkingExitResponse checkVehicle(ParkingExitCheckRequest request) {
        String licensePlate = normalizePlate(request == null ? null : request.getLicensePlate());
        if (licensePlate.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Vui lòng nhập biển số xe");
        }

        ParkingExitRow row = findActiveOrderByPlate(licensePlate, false)
                .orElseThrow(() -> new AppException(
                        HttpStatus.NOT_FOUND,
                        "Không tìm thấy phiên gửi xe đang hoạt động cho biển số " + licensePlate
                ));
        return toResponse(row, LocalDateTime.now(), false);
    }

    @Transactional
    public ParkingExitResponse confirmExit(Long orderId, ParkingExitConfirmRequest request, Long staffUserId) {
        ParkingExitRow row = findActiveOrderById(orderId, true)
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT,
                        "Phiên gửi xe không còn hoạt động hoặc đã được nhân viên khác xử lý"
                ));

        LocalDateTime exitTime = LocalDateTime.now();
        ParkingExitResponse preview = toResponse(row, exitTime, false);
        boolean visitor = "VISITOR".equals(row.getEntryType());

        if (visitor && !Boolean.TRUE.equals(request.getPaymentConfirmed())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Cần xác nhận đã nhận đủ tiền trước khi cho xe ra");
        }

        String paymentMethod = normalizePaymentMethod(request.getPaymentMethod());
        BigDecimal amount = preview.getFee().getAmount();
        String paymentStatus = visitor ? "PAID" : "NOT_REQUIRED";
        String feeBreakdown = feeBreakdownJson(preview);

        int updated = jdbcTemplate.update("""
                UPDATE parking_orders
                   SET exit_time = ?,
                       calculated_fee = ?,
                       parking_status = 'COMPLETED',
                       checked_out_by = ?,
                       checkout_confirmed_at = ?,
                       payment_status = ?,
                       payment_method = ?,
                       fee_rate_id = ?,
                       fee_breakdown = ?::jsonb,
                       updated_at = now()
                 WHERE order_id = ?
                   AND parking_status = 'ACTIVE'
                """,
                exitTime,
                amount,
                staffUserId,
                exitTime,
                paymentStatus,
                visitor ? paymentMethod : null,
                preview.getFee().getFeeRateId(),
                feeBreakdown,
                orderId
        );

        if (updated != 1) {
            throw new AppException(HttpStatus.CONFLICT, "Phiên gửi xe vừa được xử lý bởi nhân viên khác");
        }

        if (visitor) {
            jdbcTemplate.update("""
                    INSERT INTO parking_order_payments (
                        order_id, amount, payment_method, payment_status,
                        received_by, paid_at, created_at, updated_at
                    )
                    VALUES (?, ?, ?, 'PAID', ?, ?, now(), now())
                    """, orderId, amount, paymentMethod, staffUserId, exitTime);

            jdbcTemplate.update("""
                    UPDATE visitor_cards
                       SET status = 'AVAILABLE', current_order_id = NULL, updated_at = now()
                     WHERE current_order_id = ? OR visitor_card_id = ?
                    """, orderId, row.getVisitorCardId());
        }

        ParkingExitResponse response = toResponse(row, exitTime, true);
        response.setParkingStatus("COMPLETED");
        response.setExitTime(exitTime);
        response.setCanConfirmExit(false);
        response.setMessage(visitor
                ? "Đã xác nhận thanh toán và hoàn tất xe ra"
                : "Đã xác nhận xe có gói ra bãi với phí 0 VNĐ");
        return response;
    }

    private Optional<ParkingExitRow> findActiveOrderByPlate(String licensePlate, boolean forUpdate) {
        String sql = baseOrderQuery() + """
                 WHERE regexp_replace(upper(po.license_plate), '[^A-Z0-9]', '', 'g') = ?
                   AND po.parking_status = 'ACTIVE'
                 ORDER BY po.entry_time DESC
                 LIMIT 1
                """ + (forUpdate ? " FOR UPDATE OF po" : "");
        return queryOrder(sql, compactPlate(licensePlate));
    }

    private Optional<ParkingExitRow> findActiveOrderById(Long orderId, boolean forUpdate) {
        String sql = baseOrderQuery() + """
                 WHERE po.order_id = ?
                   AND po.parking_status = 'ACTIVE'
                """ + (forUpdate ? " FOR UPDATE OF po" : "");
        return queryOrder(sql, orderId);
    }

    private String baseOrderQuery() {
        return """
                SELECT po.order_id,
                       po.order_code,
                       po.license_plate,
                       po.vehicle_id,
                       po.vehicle_type_id,
                       po.parking_id,
                       po.entry_time,
                       po.parking_status,
                       COALESCE(po.entry_type,
                           CASE WHEN po.notes LIKE 'ENTRY_TYPE=MONTHLY%%' THEN 'SUBSCRIPTION' ELSE 'VISITOR' END
                       ) AS entry_type,
                       v.brand,
                       v.color,
                       vt.type_name AS vehicle_type,
                       u.full_name AS customer_name,
                       vc.visitor_card_id,
                       vc.card_code AS visitor_card_code,
                       fs.fee_subscription_id AS subscription_id,
                       fs.start_date AS subscription_start_date,
                       fs.end_date AS subscription_end_date,
                       fs.status AS subscription_status,
                       fp.name AS package_name
                  FROM parking_orders po
                  LEFT JOIN vehicles v ON v.vehicle_id = po.vehicle_id
                  LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = COALESCE(po.vehicle_type_id, v.vehicle_type_id)
                  LEFT JOIN customers c ON c.customer_id = v.customer_id
                  LEFT JOIN users u ON u.user_id = c.user_id
                  LEFT JOIN visitor_cards vc
                    ON vc.visitor_card_id = po.visitor_card_id
                    OR (po.visitor_card_id IS NULL AND vc.current_order_id = po.order_id)
                  LEFT JOIN fee_subscription fs ON fs.fee_subscription_id = po.subscription_id
                  LEFT JOIN fee_package fp ON fp.fee_package_id = fs.fee_package_id
                """;
    }

    private Optional<ParkingExitRow> queryOrder(String sql, Object... args) {
        List<ParkingExitRow> rows = jdbcTemplate.query(sql, (rs, rowNum) -> ParkingExitRow.builder()
                .orderId(rs.getLong("order_id"))
                .orderCode(rs.getString("order_code"))
                .licensePlate(rs.getString("license_plate"))
                .vehicleId(nullableLong(rs, "vehicle_id"))
                .vehicleTypeId(nullableLong(rs, "vehicle_type_id"))
                .parkingId(nullableLong(rs, "parking_id"))
                .entryTime(rs.getTimestamp("entry_time") == null ? null : rs.getTimestamp("entry_time").toLocalDateTime())
                .parkingStatus(rs.getString("parking_status"))
                .entryType(rs.getString("entry_type"))
                .brand(rs.getString("brand"))
                .color(rs.getString("color"))
                .vehicleType(rs.getString("vehicle_type"))
                .customerName(rs.getString("customer_name"))
                .visitorCardId(nullableLong(rs, "visitor_card_id"))
                .visitorCardCode(rs.getString("visitor_card_code"))
                .subscriptionId(nullableLong(rs, "subscription_id"))
                .subscriptionStartDate(rs.getTimestamp("subscription_start_date") == null
                        ? null : rs.getTimestamp("subscription_start_date").toLocalDateTime())
                .subscriptionEndDate(rs.getTimestamp("subscription_end_date") == null
                        ? null : rs.getTimestamp("subscription_end_date").toLocalDateTime())
                .subscriptionStatus(rs.getString("subscription_status"))
                .packageName(rs.getString("package_name"))
                .build(), args);
        return rows.stream().findFirst();
    }

    private ParkingExitResponse toResponse(ParkingExitRow row, LocalDateTime exitTime, boolean completed) {
        LocalDateTime entryTime = row.getEntryTime() == null ? exitTime : row.getEntryTime();
        long durationMinutes = Math.max(1, Duration.between(entryTime, exitTime).toMinutes());
        boolean visitor = "VISITOR".equals(row.getEntryType());
        ParkingExitResponse.FeeInfo fee = visitor
                ? calculateVisitorFee(row, entryTime, exitTime, durationMinutes)
                : freeSubscriptionFee();

        ParkingExitResponse.SubscriptionInfo subscription = visitor ? null
                : ParkingExitResponse.SubscriptionInfo.builder()
                .subscriptionId(row.getSubscriptionId())
                .packageName(row.getPackageName())
                .startDate(row.getSubscriptionStartDate())
                .endDate(row.getSubscriptionEndDate())
                .status(row.getSubscriptionStatus())
                .build();

        return ParkingExitResponse.builder()
                .orderId(row.getOrderId())
                .orderCode(row.getOrderCode())
                .exitType(row.getEntryType())
                .licensePlate(row.getLicensePlate())
                .vehicleType(row.getVehicleType())
                .brand(row.getBrand())
                .color(row.getColor())
                .customerName(row.getCustomerName())
                .visitorCardCode(row.getVisitorCardCode())
                .entryTime(row.getEntryTime())
                .exitTime(completed ? exitTime : null)
                .durationMinutes(durationMinutes)
                .parkingStatus(completed ? "COMPLETED" : row.getParkingStatus())
                .subscription(subscription)
                .fee(fee)
                .canConfirmExit(!completed)
                .message(visitor ? "Xe vãng lai - cần xác nhận đã nhận tiền" : "Xe có gói - phí ra bãi 0 VNĐ")
                .build();
    }

    private ParkingExitResponse.FeeInfo calculateVisitorFee(
            ParkingExitRow row, LocalDateTime entryTime, LocalDateTime exitTime, long durationMinutes) {
        FeeRate rate = findFeeRate(row.getVehicleTypeId(), row.getParkingId(), exitTime)
                .orElseThrow(() -> new AppException(
                        HttpStatus.CONFLICT,
                        "Chưa cấu hình bảng giá xe vãng lai cho loại xe này"
                ));

        long remainingMinutes = Math.max(0, durationMinutes - rate.getFirstBlockMinutes());
        int additionalBlocks = remainingMinutes == 0 ? 0
                : (int) Math.ceil((double) remainingMinutes / rate.getNextBlockMinutes());
        BigDecimal additionalFee = rate.getNextBlockFee().multiply(BigDecimal.valueOf(additionalBlocks));
        BigDecimal amount = rate.getFirstBlockFee().add(additionalFee);

        long overnightCount = Math.max(0, ChronoUnit.DAYS.between(entryTime.toLocalDate(), exitTime.toLocalDate()));
        amount = amount.add(rate.getOvernightFee().multiply(BigDecimal.valueOf(overnightCount)));

        if (rate.getDailyCap() != null) {
            long chargeDays = Math.max(1, (long) Math.ceil((double) durationMinutes / (24 * 60)));
            BigDecimal maximum = rate.getDailyCap().multiply(BigDecimal.valueOf(chargeDays));
            amount = amount.min(maximum);
        }

        return ParkingExitResponse.FeeInfo.builder()
                .required(true)
                .feeRateId(rate.getFeeRateId())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .currency("VND")
                .description("Tính theo thời gian gửi thực tế, làm tròn lên theo mỗi khung phí")
                .firstBlockMinutes(rate.getFirstBlockMinutes())
                .firstBlockFee(rate.getFirstBlockFee())
                .nextBlockMinutes(rate.getNextBlockMinutes())
                .nextBlockFee(rate.getNextBlockFee())
                .additionalBlocks(additionalBlocks)
                .additionalFee(additionalFee)
                .dailyCap(rate.getDailyCap())
                .build();
    }

    private Optional<FeeRate> findFeeRate(Long vehicleTypeId, Long parkingId, LocalDateTime at) {
        if (vehicleTypeId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.query("""
                SELECT fee_rate_id, first_block_minutes, first_block_fee,
                       next_block_minutes, next_block_fee, daily_cap, overnight_fee
                  FROM visitor_fee_rates
                 WHERE vehicle_type_id = ?
                   AND is_active = true
                   AND (parking_id = ? OR parking_id IS NULL)
                   AND effective_from <= ?
                   AND (effective_to IS NULL OR effective_to > ?)
                 ORDER BY CASE WHEN parking_id = ? THEN 0 ELSE 1 END, effective_from DESC
                 LIMIT 1
                """, (rs, rowNum) -> FeeRate.builder()
                .feeRateId(rs.getLong("fee_rate_id"))
                .firstBlockMinutes(rs.getInt("first_block_minutes"))
                .firstBlockFee(rs.getBigDecimal("first_block_fee"))
                .nextBlockMinutes(rs.getInt("next_block_minutes"))
                .nextBlockFee(rs.getBigDecimal("next_block_fee"))
                .dailyCap(rs.getBigDecimal("daily_cap"))
                .overnightFee(rs.getBigDecimal("overnight_fee"))
                .build(), vehicleTypeId, parkingId, at, at, parkingId).stream().findFirst();
    }

    private ParkingExitResponse.FeeInfo freeSubscriptionFee() {
        return ParkingExitResponse.FeeInfo.builder()
                .required(false)
                .amount(BigDecimal.ZERO.setScale(2))
                .currency("VND")
                .description("Miễn phí theo gói gửi xe đã ghi nhận khi xe vào")
                .additionalBlocks(0)
                .additionalFee(BigDecimal.ZERO.setScale(2))
                .build();
    }

    private String feeBreakdownJson(ParkingExitResponse response) {
        ParkingExitResponse.FeeInfo fee = response.getFee();
        return String.format(Locale.ROOT,
                "{\"durationMinutes\":%d,\"firstBlockMinutes\":%s,\"firstBlockFee\":%s," +
                        "\"additionalBlocks\":%s,\"additionalFee\":%s,\"total\":%s}",
                response.getDurationMinutes(),
                fee.getFirstBlockMinutes() == null ? "null" : fee.getFirstBlockMinutes(),
                fee.getFirstBlockFee() == null ? "0" : fee.getFirstBlockFee().toPlainString(),
                fee.getAdditionalBlocks() == null ? "0" : fee.getAdditionalBlocks(),
                fee.getAdditionalFee() == null ? "0" : fee.getAdditionalFee().toPlainString(),
                fee.getAmount().toPlainString());
    }

    private Long nullableLong(java.sql.ResultSet rs, String column) throws java.sql.SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String normalizePlate(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String compactPlate(String value) {
        return normalizePlate(value).replaceAll("[^A-Z0-9]", "");
    }

    private String normalizePaymentMethod(String value) {
        String normalized = value == null ? "CASH" : value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "MOMO", "BANK_TRANSFER" -> normalized;
            default -> "CASH";
        };
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class ParkingExitRow {
        private Long orderId;
        private String orderCode;
        private String licensePlate;
        private Long vehicleId;
        private Long vehicleTypeId;
        private Long parkingId;
        private LocalDateTime entryTime;
        private String parkingStatus;
        private String entryType;
        private String brand;
        private String color;
        private String vehicleType;
        private String customerName;
        private Long visitorCardId;
        private String visitorCardCode;
        private Long subscriptionId;
        private LocalDateTime subscriptionStartDate;
        private LocalDateTime subscriptionEndDate;
        private String subscriptionStatus;
        private String packageName;
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class FeeRate {
        private Long feeRateId;
        private Integer firstBlockMinutes;
        private BigDecimal firstBlockFee;
        private Integer nextBlockMinutes;
        private BigDecimal nextBlockFee;
        private BigDecimal dailyCap;
        private BigDecimal overnightFee;
    }
}
