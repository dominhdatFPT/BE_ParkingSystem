package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SecurityRoleService {

    public void requireAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Set<String> expectedAuthorities = Arrays.stream(roles)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toSet());

        boolean matched = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expectedAuthorities::contains);

        if (!matched) {
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
        }
    }
}
