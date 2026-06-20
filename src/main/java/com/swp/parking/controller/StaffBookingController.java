package com.swp.parking.controller;

import com.swp.parking.dto.request.StaffBookingDecisionRequest;
import com.swp.parking.dto.response.BookingResponse;
import com.swp.parking.service.BookingService;
import com.swp.parking.service.SecurityRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/bookings")
@RequiredArgsConstructor
public class StaffBookingController {

    private final BookingService bookingService;
    private final SecurityRoleService securityRoleService;

    @GetMapping("/pending")
    public ResponseEntity<List<BookingResponse>> getPendingBookings() {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        return ResponseEntity.ok(bookingService.getPendingStaffBookings());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<BookingResponse> approveBooking(
            @PathVariable Long id,
            @RequestBody(required = false) StaffBookingDecisionRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        Long staffId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingService.approveBooking(id, staffId, request));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<BookingResponse> rejectBooking(
            @PathVariable Long id,
            @RequestBody(required = false) StaffBookingDecisionRequest request) {
        securityRoleService.requireAnyRole("ADMIN", "STAFF");
        Long staffId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(bookingService.rejectBooking(id, staffId, request));
    }
}
