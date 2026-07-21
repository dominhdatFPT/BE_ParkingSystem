package com.swp.parking.repository;

import com.swp.parking.dto.response.AccountUserResponse;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountUserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    @Query("""
            SELECT new com.swp.parking.dto.response.AccountUserResponse(
                u.id, u.fullName, u.email, u.phone, u.avatarUrl, u.status, u.role, u.createdAt
            )
              FROM User u
             WHERE u.role = :role
               AND (:status IS NULL OR u.status = :status)
               AND (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AccountUserResponse> searchAccountUsers(
            @Param("role") UserRole role,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT new com.swp.parking.dto.response.AccountUserResponse(
                u.id, u.fullName, u.email, u.phone, u.avatarUrl, u.status, u.role, u.createdAt
            )
              FROM User u
             WHERE u.role IN :roles
               AND (:role IS NULL OR u.role = :role)
               AND (:status IS NULL OR u.status = :status)
               AND (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AccountUserResponse> searchAccountStaffs(
            @Param("roles") List<UserRole> roles,
            @Param("role") UserRole role,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query(value = """
            SELECT
                u.user_id AS userId,
                reg.license_plate AS licensePlate,
                reg.fee_package_name AS feePackageName,
                CASE
                    WHEN sub.status IN ('ACTIVE', 'PAID') THEN 'Đã thanh toán'
                    WHEN reg.requested_fee_package_id IS NOT NULL OR sub.status = 'PENDING_PAYMENT' THEN 'Chưa thanh toán'
                    ELSE 'Chưa đăng ký gói'
                END AS cardStatus
            FROM users u
            LEFT JOIN LATERAL (
                SELECT vr.registration_id,
                       vr.license_plate,
                       vr.vehicle_id,
                       vr.requested_fee_package_id,
                       fp.name AS fee_package_name
                FROM vehicle_registrations vr
                LEFT JOIN fee_package fp ON vr.requested_fee_package_id = fp.fee_package_id
                WHERE vr.user_id = u.user_id AND vr.is_deleted = false
                ORDER BY vr.created_at DESC
                LIMIT 1
            ) reg ON true
            LEFT JOIN LATERAL (
                SELECT fs.status
                FROM fee_subscription fs
                WHERE fs.vehicle_id = reg.vehicle_id
                ORDER BY fs.created_at DESC
                LIMIT 1
            ) sub ON true
            WHERE u.user_id IN (:userIds)
            """, nativeQuery = true)
    List<CardInfoProjection> findCardInfoByUserIds(@Param("userIds") List<Long> userIds);

    interface CardInfoProjection {
        Long getUserId();
        String getLicensePlate();
        String getFeePackageName();
        String getCardStatus();
    }

    @Query(value = """
            SELECT
                u.user_id            AS userId,
                v.vehicle_id         AS vehicleId,
                v.license_plate      AS licensePlate,
                v.brand              AS brand,
                v.color              AS color,
                vt.type_name         AS vehicleTypeName,
                vt.type_code         AS vehicleTypeCode,
                fs.fee_subscription_id AS subscriptionId,
                fs.status            AS subscriptionStatus,
                fs.amount_to_pay     AS amountToPay,
                fs.start_date        AS startDate,
                fs.end_date          AS endDate,
                fp.name              AS feePackageName,
                CASE
                    WHEN fs.fee_subscription_id IS NULL THEN 'NOT_SUBSCRIBED'
                    WHEN fs.status = 'ACTIVE'  THEN 'PAID'
                    WHEN fs.status = 'PENDING_PAYMENT' THEN 'UNPAID'
                    WHEN fs.status = 'CANCELLED' THEN 'CANCELLED'
                    ELSE 'UNPAID'
                END AS paymentStatus,
                CASE
                    WHEN fs.fee_subscription_id IS NULL THEN 'Chua dang ky goi'
                    WHEN fs.status = 'ACTIVE'  THEN 'Da thanh toan'
                    WHEN fs.status = 'PENDING_PAYMENT' THEN 'Chua thanh toan'
                    WHEN fs.status = 'CANCELLED' THEN 'Da huy'
                    ELSE 'Chua thanh toan'
                END AS paymentStatusLabel
            FROM users u
            JOIN customers c ON c.user_id = u.user_id
            JOIN vehicles v ON v.customer_id = c.customer_id
            LEFT JOIN vehicle_types vt ON vt.vehicle_type_id = v.vehicle_type_id
            LEFT JOIN LATERAL (
                SELECT sub.*
                FROM fee_subscription sub
                WHERE sub.vehicle_id = v.vehicle_id
                ORDER BY sub.created_at DESC
                LIMIT 1
            ) fs ON true
            LEFT JOIN fee_package fp ON fp.fee_package_id = fs.fee_package_id
            WHERE u.user_id IN (:userIds)
              AND fs.status IN ('ACTIVE', 'PENDING_PAYMENT')
            ORDER BY u.user_id, v.created_at DESC
            """, nativeQuery = true)
    List<VehicleInfoProjection> findVehicleInfoByUserIds(@Param("userIds") List<Long> userIds);

    interface VehicleInfoProjection {
        Long getUserId();
        Long getVehicleId();
        String getLicensePlate();
        String getBrand();
        String getColor();
        String getVehicleTypeName();
        String getVehicleTypeCode();
        Long getSubscriptionId();
        String getSubscriptionStatus();
        java.math.BigDecimal getAmountToPay();
        java.time.LocalDateTime getStartDate();
        java.time.LocalDateTime getEndDate();
        String getFeePackageName();
        String getPaymentStatus();
        String getPaymentStatusLabel();
    }
}
