package com.swp.parking.repository;

import com.swp.parking.model.VehicleRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleRegistrationRepository extends JpaRepository<VehicleRegistration, Long> {

    List<VehicleRegistration> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Page<VehicleRegistration> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<VehicleRegistration> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    boolean existsByUser_IdAndLicensePlate(Long userId, String licensePlate);

    boolean existsByUser_IdAndLicensePlateAndStatusIn(Long userId, String licensePlate, List<String> statuses);

    boolean existsByEkycCccdIdAndUser_IdNot(String ekycCccdId, Long userId);

    boolean existsByEkycLicenseNumberAndUser_IdNot(String ekycLicenseNumber, Long userId);
}
