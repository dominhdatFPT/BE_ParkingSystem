package com.swp.parking.service;

import com.swp.parking.dto.request.BookingRequest;
import com.swp.parking.dto.request.BookingPaymentRequest;
import com.swp.parking.dto.request.StaffBookingDecisionRequest;
import com.swp.parking.dto.request.UserBookingRequest;
import com.swp.parking.dto.response.BookingResponse;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.Booking;
import com.swp.parking.model.Card;
import com.swp.parking.model.ParkingFacility;
import com.swp.parking.model.ParkingFloor;
import com.swp.parking.model.ParkingSlot;
import com.swp.parking.model.ParkingZone;
import com.swp.parking.model.User;
import com.swp.parking.model.VehicleType;
import com.swp.parking.model.enums.BookingStatus;
import com.swp.parking.model.enums.PaymentStatus;
import com.swp.parking.model.enums.ParkingSlotStatus;
import com.swp.parking.repository.BookingRepository;
import com.swp.parking.repository.ParkingFacilityRepository;
import com.swp.parking.repository.ParkingFloorRepository;
import com.swp.parking.repository.ParkingSlotRepository;
import com.swp.parking.repository.ParkingZoneRepository;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.repository.VehicleTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ParkingSlotRepository parkingSlotRepository;
    private final ParkingFacilityRepository parkingFacilityRepository;
    private final ParkingFloorRepository parkingFloorRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final ParkingZoneRepository parkingZoneRepository;
    private final CardService cardService;

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long id) {
        Booking booking = bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));
        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(Long userId) {
        return bookingRepository.findUserBookingsWithDetails(userId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getPendingStaffBookings() {
        return bookingRepository.findAllByStatusWithDetails(BookingStatus.WAITING_STAFF_APPROVAL).stream()
                .map(this::mapToResponse)
                .toList();
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
                .paymentStatus(resolvePaymentStatus(status))
                .build();

        booking = bookingRepository.save(booking);

        // TODO: Cập nhật trạng thái slot và gửi push notification qua Firebase
        if (status == BookingStatus.CONFIRMED) {
            slot.setStatus(ParkingSlotStatus.RESERVED);
            parkingSlotRepository.save(slot);
        }

        return mapToResponse(booking);
    }

    public BookingResponse createUserBooking(Long userId, UserBookingRequest request) {
        validateBookingTime(request.getStartTime(), request.getEndTime());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
        Card card = cardService.ensureActiveUserCard(userId);
        ParkingFacility facility = parkingFacilityRepository.findById(request.getParkingId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking facility not found"));
        ParkingFloor floor = parkingFloorRepository.findById(request.getFloorId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Parking floor not found"));
        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Vehicle type not found"));

        if (floor.getParkingFacility() == null || !floor.getParkingFacility().getId().equals(facility.getId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Selected floor does not belong to the selected facility");
        }

        ParkingZone zone = parkingZoneRepository
                .findByParkingFacilityIdAndParkingFloorIdAndVehicleTypeId(
                        facility.getId(),
                        floor.getId(),
                        vehicleType.getId())
                .orElse(null);

        Booking booking = Booking.builder()
                .user(user)
                .card(card)
                .parkingFacility(facility)
                .parkingFloor(floor)
                .vehicleType(vehicleType)
                .parkingZone(zone)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.WAITING_STAFF_APPROVAL)
                .paymentStatus(PaymentStatus.NOT_ALLOWED)
                .build();

        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse approveBooking(Long bookingId, Long staffId, StaffBookingDecisionRequest request) {
        Booking booking = findBookingForStaffDecision(bookingId);
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Staff user not found"));

        booking.setStatus(BookingStatus.APPROVED_WAITING_PAYMENT);
        booking.setPaymentStatus(PaymentStatus.UNPAID);
        booking.setAcceptedBy(staff);
        booking.setAcceptedAt(LocalDateTime.now());
        booking.setStaffNote(request != null ? request.getNote() : null);

        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse rejectBooking(Long bookingId, Long staffId, StaffBookingDecisionRequest request) {
        Booking booking = findBookingForStaffDecision(bookingId);
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Staff user not found"));

        booking.setStatus(BookingStatus.REJECTED);
        booking.setPaymentStatus(PaymentStatus.NOT_ALLOWED);
        booking.setAcceptedBy(staff);
        booking.setStaffNote(request != null ? request.getNote() : null);
        booking.setRejectedAt(LocalDateTime.now());

        return mapToResponse(bookingRepository.save(booking));
    }

    public BookingResponse payBooking(Long bookingId, Long userId, BookingPaymentRequest request) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (!booking.getUser().getId().equals(userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "You can only pay for your own booking");
        }

        if (booking.getStatus() != BookingStatus.APPROVED_WAITING_PAYMENT
                || booking.getPaymentStatus() != PaymentStatus.UNPAID) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "You can only pay after staff has approved this booking");
        }

        booking.setStatus(BookingStatus.PAID);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setPaidAt(LocalDateTime.now());

        return mapToResponse(bookingRepository.save(booking));
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
            booking.setPaymentStatus(resolvePaymentStatus(request.getStatus()));
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
        User user = booking.getUser();
        Card card = booking.getCard();
        ParkingSlot slot = booking.getParkingSlot();
        ParkingFacility facility = booking.getParkingFacility();
        ParkingFloor floor = booking.getParkingFloor();
        VehicleType vehicleType = booking.getVehicleType();
        ParkingZone zone = booking.getParkingZone();
        User acceptedBy = booking.getAcceptedBy();

        return BookingResponse.builder()
                .id(booking.getId())
                .userId(user != null ? user.getId() : null)
                .userFullName(user != null ? user.getFullName() : null)
                .cardId(card != null ? card.getId() : null)
                .cardCode(card != null ? card.getCardCode() : null)
                .parkingSlotId(slot != null ? slot.getId() : null)
                .slotNumber(slot != null ? slot.getSlotNumber() : null)
                .parkingId(facility != null ? facility.getId() : null)
                .parkingName(facility != null ? facility.getParkingName() : null)
                .floorId(floor != null ? floor.getId() : null)
                .floorName(floor != null ? floor.getFloorName() : null)
                .vehicleTypeId(vehicleType != null ? vehicleType.getId() : null)
                .vehicleTypeName(vehicleType != null ? vehicleType.getTypeName() : null)
                .zoneId(zone != null ? zone.getId() : null)
                .zoneName(zone != null ? zone.getZoneName() : null)
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .acceptedBy(acceptedBy != null ? acceptedBy.getId() : null)
                .acceptedByName(acceptedBy != null ? acceptedBy.getFullName() : null)
                .acceptedAt(booking.getAcceptedAt())
                .staffNote(booking.getStaffNote())
                .rejectedAt(booking.getRejectedAt())
                .paidAt(booking.getPaidAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private Booking findBookingForStaffDecision(Long bookingId) {
        Booking booking = bookingRepository.findByIdWithDetails(bookingId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Booking not found"));

        if (booking.getStatus() != BookingStatus.WAITING_STAFF_APPROVAL) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Only waiting bookings can be approved or rejected");
        }

        return booking;
    }

    private void validateBookingTime(LocalDateTime startTime, LocalDateTime endTime) {
        if (endTime.isBefore(startTime) || endTime.isEqual(startTime)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "End time must be after start time");
        }
    }

    private PaymentStatus resolvePaymentStatus(BookingStatus status) {
        if (status == BookingStatus.APPROVED_WAITING_PAYMENT || status == BookingStatus.CONFIRMED) {
            return PaymentStatus.UNPAID;
        }
        if (status == BookingStatus.PAID || status == BookingStatus.COMPLETED) {
            return PaymentStatus.PAID;
        }
        return PaymentStatus.NOT_ALLOWED;
    }
}
