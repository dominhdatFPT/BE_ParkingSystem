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
                    WHEN reg.registration_id IS NULL THEN 'CHƯA ĐĂNG KÝ'
                    WHEN sub.status IS NULL OR sub.status NOT IN ('ACTIVE', 'PAID') THEN 'CHƯA THANH TOÁN'
                    ELSE 'ĐÃ THANH TOÁN'
                END AS cardStatus
            FROM users u
            LEFT JOIN LATERAL (
                SELECT vr.registration_id, vr.license_plate, vr.vehicle_id, fp.name AS fee_package_name
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
}
