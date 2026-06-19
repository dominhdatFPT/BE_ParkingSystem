package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SecurityRoleService {

    public void requireAnyRole(String... roles) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Authentication is required");
        }

        Set<String> expectedAuthorities = Arrays.stream(roles)
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toSet());

        Set<String> actualAuthorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        log.debug("Role check - principal: {}, expected: {}, actual: {}",
                authentication.getPrincipal(), expectedAuthorities, actualAuthorities);

        boolean matched = actualAuthorities.stream().anyMatch(expectedAuthorities::contains);

        if (!matched) {
            log.warn("Access denied - principal: {}, expected: {}, actual: {}",
                    authentication.getPrincipal(), expectedAuthorities, actualAuthorities);
            throw new AppException(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
        }
    }
}
