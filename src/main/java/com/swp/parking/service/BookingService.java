package com.swp.parking.service;

import com.swp.parking.dto.request.BookingRequest;
import com.swp.parking.dto.response.BookingResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.Booking;
import com.swp.parking.model.ParkingSlot;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.BookingStatus;
import com.swp.parking.model.enums.ParkingSlotStatus;
import com.swp.parking.repository.BookingRepository;
import com.swp.parking.repository.ParkingSlotRepository;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));
        return mapToResponse(booking);
    }

    public BookingResponse createBooking(BookingRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        ParkingSlot slot = parkingSlotRepository.findById(request.getParkingSlotId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking slot not found"));

        if (slot.getStatus() != ParkingSlotStatus.AVAILABLE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Parking slot is not available");
        }

        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }

        BookingStatus status = request.getStatus() != null ? request.getStatus() : BookingStatus.PENDING;

        Booking booking = Booking.builder()
                .user(user)
                .parkingSlot(slot)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(status)
                .build();

        booking = bookingRepository.save(booking);

        // TODO: Cập nhật trạng thái slot và gửi push notification qua Firebase
        if (status == BookingStatus.CONFIRMED) {
            slot.setStatus(ParkingSlotStatus.RESERVED);
            parkingSlotRepository.save(slot);
        }

        return mapToResponse(booking);
    }

    public BookingResponse updateBooking(Long id, BookingRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));

        ParkingSlot slot = parkingSlotRepository.findById(request.getParkingSlotId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking slot not found"));

        booking.setUser(user);
        booking.setParkingSlot(slot);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());

        if (request.getStatus() != null) {
            booking.setStatus(request.getStatus());
        }

        return mapToResponse(bookingRepository.save(booking));
    }

    public void deleteBooking(Long id) {
        if (!bookingRepository.existsById(id)) {
            throw new AppException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        bookingRepository.deleteById(id);
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userFullName(booking.getUser().getFullName())
                .parkingSlotId(booking.getParkingSlot().getId())
                .slotNumber(booking.getParkingSlot().getSlotNumber())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
