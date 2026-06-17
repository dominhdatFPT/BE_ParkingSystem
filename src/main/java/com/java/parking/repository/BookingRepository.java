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

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByParkingZoneIdAndStatusIn(Long parkingZoneId, List<BookingStatus> statuses);

    @Query("""
        SELECT b
        FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.card
        LEFT JOIN FETCH b.parkingSlot
        LEFT JOIN FETCH b.parkingFacility
        LEFT JOIN FETCH b.parkingFloor
        LEFT JOIN FETCH b.vehicleType
        LEFT JOIN FETCH b.parkingZone
        LEFT JOIN FETCH b.acceptedBy
        WHERE b.status = :status
        ORDER BY b.createdAt ASC
        """)
    List<Booking> findAllByStatusWithDetails(@Param("status") BookingStatus status);

    @Query("""
        SELECT b
        FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.card
        LEFT JOIN FETCH b.parkingSlot
        LEFT JOIN FETCH b.parkingFacility
        LEFT JOIN FETCH b.parkingFloor
        LEFT JOIN FETCH b.vehicleType
        LEFT JOIN FETCH b.parkingZone
        LEFT JOIN FETCH b.acceptedBy
        WHERE b.user.id = :userId
        ORDER BY b.createdAt DESC
        """)
    List<Booking> findUserBookingsWithDetails(@Param("userId") Long userId);

    @Query("""
        SELECT b
        FROM Booking b
        LEFT JOIN FETCH b.user
        LEFT JOIN FETCH b.card
        LEFT JOIN FETCH b.parkingSlot
        LEFT JOIN FETCH b.parkingFacility
        LEFT JOIN FETCH b.parkingFloor
        LEFT JOIN FETCH b.vehicleType
        LEFT JOIN FETCH b.parkingZone
        LEFT JOIN FETCH b.acceptedBy
        WHERE b.id = :bookingId
        """)
    java.util.Optional<Booking> findByIdWithDetails(@Param("bookingId") Long bookingId);
}
