package com.swp.parking.dto.auth;

public record SessionStatusResponse(
        boolean loggedIn,
        String role,
        boolean hasStaffAccess
) {
}
