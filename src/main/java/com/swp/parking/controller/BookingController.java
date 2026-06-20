package com.swp.parking.controller;

import com.swp.parking.dto.request.BookingRequest;
import com.swp.parking.dto.request.BookingPaymentRequest;
import com.swp.parking.dto.request.UserBookingRequest;
import com.swp.parking.dto.response.BookingResponse;
import com.swp.parking.service.BookingService;
import com.swp.parking.service.SecurityRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final SecurityRoleService securityRoleService;

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(bookingService.getAllBookings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBookingById(@PathVariable Long id) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(bookingService.getBookingById(id));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<BookingResponse>> getMyBookings() {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingService.getMyBookings(userId));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> createUserBooking(@Valid @RequestBody UserBookingRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        BookingResponse response = bookingService.createUserBooking(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin-create")
    public ResponseEntity<BookingResponse> createBooking(@Valid @RequestBody BookingRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/payment")
    public ResponseEntity<BookingResponse> payBooking(
            @PathVariable Long id,
            @RequestBody(required = false) BookingPaymentRequest request) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingService.payBooking(id, userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookingResponse> updateBooking(
            @PathVariable Long id,
            @Valid @RequestBody BookingRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(bookingService.updateBooking(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBooking(@PathVariable Long id) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        bookingService.deleteBooking(id);
        return ResponseEntity.noContent().build();
    }
}
