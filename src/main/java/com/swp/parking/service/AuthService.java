package com.swp.parking.service;

import com.swp.parking.dto.auth.SessionStatusResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final List<String> ROLE_PRIORITY = List.of("ADMIN", "STAFF", "USER");

    public SessionStatusResponse getSessionStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return new SessionStatusResponse(false, null, false);
        }

        String role = extractHighestRole(authentication);
        boolean hasStaffAccess = "STAFF".equals(role) || "ADMIN".equals(role);
        return new SessionStatusResponse(true, role, hasStaffAccess);
    }

    private String extractHighestRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::normalizeRole)
                .filter(role -> !role.isBlank())
                .sorted(Comparator.comparingInt(this::rolePriority))
                .findFirst()
                .orElse("USER");
    }

    private String normalizeRole(String authority) {
        if (authority == null || authority.isBlank()) {
            return "";
        }
        if (authority.startsWith("ROLE_")) {
            return authority.substring("ROLE_".length());
        }
        return authority;
    }

    private int rolePriority(String role) {
        int index = ROLE_PRIORITY.indexOf(role);
        return index == -1 ? Integer.MAX_VALUE : index;
    }
}
