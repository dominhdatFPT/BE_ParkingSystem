package com.swp.parking.repository;

import com.swp.parking.model.Booking;
import com.swp.parking.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByParkingSlotIdAndStatusIn(Long parkingSlotId, List<BookingStatus> statuses);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByParkingZoneIdAndStatusOrderByCreatedAtAsc(Long parkingZoneId, BookingStatus status);

    List<Booking> findByStatusOrderByCreatedAtAsc(BookingStatus status);

    long countByParkingZoneIdAndStatusIn(Long parkingZoneId, List<BookingStatus> statuses);

    @Query("""
        SELECT b
        FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.parkingFacility
        LEFT JOIN FETCH b.parkingFloor
        LEFT JOIN FETCH b.vehicleType
        LEFT JOIN FETCH b.parkingZone
        WHERE b.status = :status
        ORDER BY b.createdAt ASC
        """)
    List<Booking> findAllByStatusWithDetails(@Param("status") BookingStatus status);
}
