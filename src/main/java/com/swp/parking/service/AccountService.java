package com.swp.parking.service;

import com.swp.parking.dto.request.CreateAccountRequest;
import com.swp.parking.dto.response.AccountUserResponse;
import com.swp.parking.dto.response.AccountVehicleInfo;
import com.swp.parking.exception.AppException;
import com.swp.parking.exception.DuplicateEmailException;
import com.swp.parking.exception.InvalidRoleException;
import com.swp.parking.exception.NotFoundException;
import com.swp.parking.model.Customer;
import com.swp.parking.model.Employee;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import com.swp.parking.repository.AccountUserRepository;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Pattern GMAIL_EMAIL_PATTERN =
            Pattern.compile(com.swp.parking.validation.ValidationPatterns.GMAIL_EMAIL, Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN =
            Pattern.compile(com.swp.parking.validation.ValidationPatterns.OPTIONAL_VIETNAM_PHONE);
    private static final Set<UserRole> VALID_ROLES =
            Set.of(UserRole.USER, UserRole.STAFF, UserRole.ADMIN);
    private static final List<UserRole> STAFF_ROLES =
            List.of(UserRole.STAFF, UserRole.ADMIN);

    private final AccountUserRepository accountUserRepository;
    private final EmployeeRepository employeeRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public Page<AccountUserResponse> getUsers(String status, String keyword, Pageable pageable) {
        String statusFilter = normalizeFilter(status);
        String keywordFilter = normalizeFilter(keyword);
        Page<AccountUserResponse> page = accountUserRepository.searchAccountUsers(
                UserRole.USER, statusFilter, keywordFilter, pageable);

        if (page.hasContent()) {
            enrichWithCardInfo(page.getContent());
        }

        return page;
    }

    private void enrichWithCardInfo(List<AccountUserResponse> users) {
        List<Long> userIds = users.stream()
                .map(AccountUserResponse::getUserId)
                .toList();

        Map<Long, AccountUserRepository.CardInfoProjection> cardInfoMap =
                accountUserRepository.findCardInfoByUserIds(userIds)
                        .stream()
                        .collect(Collectors.toMap(
                                AccountUserRepository.CardInfoProjection::getUserId,
                                ci -> ci
                        ));

        List<AccountUserRepository.VehicleInfoProjection> vehicleProjections =
                accountUserRepository.findVehicleInfoByUserIds(userIds);

        Map<Long, List<AccountUserRepository.VehicleInfoProjection>> vehiclesByUser =
                vehicleProjections.stream()
                        .collect(Collectors.groupingBy(
                                AccountUserRepository.VehicleInfoProjection::getUserId,
                                Collectors.toList()
                        ));

        for (AccountUserResponse user : users) {
            AccountUserRepository.CardInfoProjection info = cardInfoMap.get(user.getUserId());
            if (info != null) {
                user.setCardStatus(info.getCardStatus());
                user.setFeePackageName(info.getFeePackageName());
                user.setLicensePlate(info.getLicensePlate());
            } else {
                user.setCardStatus("Chưa đăng ký gói");
            }

            List<AccountUserRepository.VehicleInfoProjection> vehicleInfos =
                    vehiclesByUser.getOrDefault(user.getUserId(), List.of());
            List<AccountVehicleInfo> vehicleList = vehicleInfos.stream()
                    .map(v -> AccountVehicleInfo.builder()
                            .vehicleId(v.getVehicleId())
                            .licensePlate(v.getLicensePlate())
                            .brand(v.getBrand())
                            .color(v.getColor())
                            .vehicleTypeName(v.getVehicleTypeName())
                            .vehicleTypeCode(v.getVehicleTypeCode())
                            .subscriptionId(v.getSubscriptionId())
                            .subscriptionStatus(v.getSubscriptionStatus())
                            .amountToPay(v.getAmountToPay())
                            .startDate(v.getStartDate())
                            .endDate(v.getEndDate())
                            .feePackageName(v.getFeePackageName())
                            .paymentStatus(v.getPaymentStatus())
                            .paymentStatusLabel(v.getPaymentStatusLabel())
                            .build())
                    .toList();
            user.setVehicles(vehicleList);
        }
    }

    public AccountUserResponse createUser(CreateAccountRequest request) {
        String email = normalizeEmail(request.getEmail());
        if (accountUserRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException("Email already exists: " + email);
        }

        UserRole role;
        try {
            role = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Role không hợp lệ. Chỉ chấp nhận USER, STAFF hoặc ADMIN");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .phone(normalizePhone(request.getPhone()))
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .status("ACTIVE")
                .build();

        user = accountUserRepository.save(user);
        ensureCustomerProfile(user);

        if (role == UserRole.STAFF || role == UserRole.ADMIN) {
            String code = generateEmployeeCode(user.getId());
            Employee emp = Employee.builder()
                    .userId(user.getId())
                    .role(role.name())
                    .employeeCode(code)
                    .status("ACTIVE")
                    .build();
            employeeRepository.save(emp);
        }

        return mapToAccountUserResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<AccountUserResponse> getStaffs(String role, String status, String keyword, Pageable pageable) {
        UserRole roleFilter = parseRoleFilter(role);
        String statusFilter = normalizeFilter(status);
        String keywordFilter = normalizeFilter(keyword);
        return accountUserRepository.searchAccountStaffs(
                STAFF_ROLES, roleFilter, statusFilter, keywordFilter, pageable);
    }

    public void updateUserStatus(Long userId) {
        User user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với id: " + userId));
        String currentStatus = user.getStatus();
        String newStatus = STATUS_ACTIVE.equalsIgnoreCase(currentStatus) ? STATUS_INACTIVE : STATUS_ACTIVE;
        user.setStatus(newStatus);
        accountUserRepository.save(user);

        // Sync with employees table
        employeeRepository.findByUserId(userId).ifPresent(emp -> {
            emp.setStatus(newStatus);
            employeeRepository.save(emp);
        });
    }

    public AccountUserResponse updateUserPhone(Long userId, String phone) {
        User user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("KhÃ´ng tÃ¬m tháº¥y user vá»›i id: " + userId));

        user.setPhone(normalizePhone(phone));
        return mapToAccountUserResponse(accountUserRepository.save(user));
    }

    public void updateUserRole(Long userId, String role) {
        String normalizedRole = role == null ? null : role.trim().toUpperCase();
        UserRole newRole;
        try {
            newRole = UserRole.valueOf(normalizedRole);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidRoleException("Role không hợp lệ. Chỉ chấp nhận USER, STAFF hoặc ADMIN");
        }
        if (!VALID_ROLES.contains(newRole)) {
            throw new InvalidRoleException("Role không hợp lệ. Chỉ chấp nhận USER, STAFF hoặc ADMIN");
        }
        User user = accountUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy user với id: " + userId));
        user.setRole(newRole);
        accountUserRepository.save(user);

        // Sync with employees table so login resolveRole() picks up the new role
        if (newRole == UserRole.STAFF || newRole == UserRole.ADMIN) {
            Optional<Employee> existing = employeeRepository.findByUserId(userId);
            if (existing.isPresent()) {
                Employee emp = existing.get();
                emp.setRole(newRole.name());
                emp.setStatus(STATUS_ACTIVE);
                employeeRepository.save(emp);
            } else {
                String code = generateEmployeeCode(userId);
                Employee emp = Employee.builder()
                        .userId(userId)
                        .role(newRole.name())
                        .employeeCode(code)
                        .status(STATUS_ACTIVE)
                        .build();
                employeeRepository.save(emp);
            }
        } else if (newRole == UserRole.USER) {
            // Downgrade to USER → deactivate the employee record
            employeeRepository.findByUserId(userId)
                    .ifPresent(emp -> {
                        emp.setStatus(STATUS_INACTIVE);
                        employeeRepository.save(emp);
                    });
        }
    }

    private Customer ensureCustomerProfile(User user) {
        return customerRepository.findByUser_Id(user.getId())
                .orElseGet(() -> customerRepository.save(Customer.builder()
                        .user(user)
                        .build()));
    }

    private AccountUserResponse mapToAccountUserResponse(User user) {
        return AccountUserResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String generateEmployeeCode(Long userId) {
        long count = employeeRepository.countByEmployeeCodeStartingWith("STF");
        return "STF" + String.format("%03d", count + 1);
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toUpperCase();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (!PHONE_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Phone number must be 10-11 digits and start with 0");
        }
        return trimmed;
    }

    private String normalizeEmail(String value) {
        String normalized = value == null ? null : value.trim().toLowerCase(Locale.ROOT);
        if (normalized == null || normalized.isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email is required");
        }
        if (!GMAIL_EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email must be a Gmail address");
        }
        return normalized;
    }

    private UserRole parseRoleFilter(String value) {
        String normalized = normalizeFilter(value);
        if (normalized == null) {
            return null;
        }
        try {
            return UserRole.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
