package com.swp.parking.repository;

import com.swp.parking.model.VehicleRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistration, Long> {

    List<VehicleRegistration> findAllByIsDeletedFalse();

    @Query("SELECT r FROM VehicleRegistration r WHERE r.id = :id AND r.isDeleted = false")
    java.util.Optional<VehicleRegistration> findByIdAndNotDeleted(@Param("id") Long id);

    boolean existsByLicensePlateAndIsDeletedFalse(String licensePlate);

    List<VehicleRegistration> findAllByUserIdAndIsDeletedFalse(Long userId);

    List<VehicleRegistration> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Page<VehicleRegistration> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<VehicleRegistration> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    @Query("""
            SELECT r.id AS registrationId,
                   u.id AS userId,
                   u.fullName AS userFullName,
                   vt.id AS vehicleTypeId,
                   vt.typeName AS vehicleTypeName,
                   r.licensePlate AS licensePlate,
                   r.contactPhone AS contactPhone,
                   fp.id AS requestedFeePackageId,
                   fp.name AS requestedFeePackageName,
                   r.registrationSource AS registrationSource,
                   r.brand AS brand,
                   r.color AS color,
                   r.status AS status,
                   r.rejectReason AS rejectReason,
                   r.ekycFullName AS ekycFullName,
                   r.ekycCccdId AS ekycCccdId,
                   r.ekycLicenseNumber AS ekycLicenseNumber,
                   r.ekycLicenseClass AS ekycLicenseClass,
                   r.ekycIsValid AS ekycIsValid,
                   r.ekycIsFake AS ekycIsFake,
                   r.ekycConfidenceScore AS ekycConfidenceScore,
                   r.createdAt AS createdAt,
                   r.reviewedAt AS reviewedAt
              FROM VehicleRegistration r
              JOIN r.user u
              JOIN r.vehicleType vt
              LEFT JOIN r.requestedFeePackage fp
             WHERE (:status IS NULL OR r.status = :status)
             ORDER BY r.createdAt DESC
            """)
    Page<VehicleRegistrationSummary> findSummaries(@Param("status") String status, Pageable pageable);

    @Query("""
            SELECT r.id AS registrationId,
                   u.id AS userId,
                   u.fullName AS userFullName,
                   vt.id AS vehicleTypeId,
                   vt.typeName AS vehicleTypeName,
                   r.licensePlate AS licensePlate,
                   r.contactPhone AS contactPhone,
                   fp.id AS requestedFeePackageId,
                   fp.name AS requestedFeePackageName,
                   r.registrationSource AS registrationSource,
                   r.brand AS brand,
                   r.color AS color,
                   r.status AS status,
                   r.rejectReason AS rejectReason,
                   r.ekycFullName AS ekycFullName,
                   r.ekycCccdId AS ekycCccdId,
                   r.ekycLicenseNumber AS ekycLicenseNumber,
                   r.ekycLicenseClass AS ekycLicenseClass,
                   r.ekycIsValid AS ekycIsValid,
                   r.ekycIsFake AS ekycIsFake,
                   r.ekycConfidenceScore AS ekycConfidenceScore,
                   r.createdAt AS createdAt,
                   r.reviewedAt AS reviewedAt
              FROM VehicleRegistration r
              JOIN r.user u
              JOIN r.vehicleType vt
              LEFT JOIN r.requestedFeePackage fp
             WHERE u.id = :userId AND r.isDeleted = false
             ORDER BY r.createdAt DESC
            """)
    List<VehicleRegistrationSummary> findSummariesByUserId(@Param("userId") Long userId);

    boolean existsByUser_IdAndLicensePlate(Long userId, String licensePlate);

    boolean existsByUser_IdAndLicensePlateAndStatusIn(Long userId, String licensePlate, List<String> statuses);

    boolean existsByEkycCccdIdAndUser_IdNot(String ekycCccdId, Long userId);

    boolean existsByEkycLicenseNumberAndUser_IdNot(String ekycLicenseNumber, Long userId);

    interface VehicleRegistrationSummary {
        Long getRegistrationId();
        Long getUserId();
        String getUserFullName();
        Long getVehicleTypeId();
        String getVehicleTypeName();
        String getLicensePlate();
        String getContactPhone();
        Long getRequestedFeePackageId();
        String getRequestedFeePackageName();
        String getRegistrationSource();
        String getBrand();
        String getColor();
        String getStatus();
        String getRejectReason();
        String getEkycFullName();
        String getEkycCccdId();
        String getEkycLicenseNumber();
        String getEkycLicenseClass();
        Boolean getEkycIsValid();
        Boolean getEkycIsFake();
        Double getEkycConfidenceScore();
        LocalDateTime getCreatedAt();
        LocalDateTime getReviewedAt();
    }
}
