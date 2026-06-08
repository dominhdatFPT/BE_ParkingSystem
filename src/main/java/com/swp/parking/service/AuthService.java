package com.swp.parking.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.swp.parking.dto.request.FirebaseLoginRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.response.LoginResponse;
import com.swp.parking.entity.Customer;
import com.swp.parking.entity.Employee;
import com.swp.parking.entity.User;
import com.swp.parking.exception.ResourceNotFoundException;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.EmployeeRepository;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service xử lý xác thực: đăng nhập customer và employee bằng email + mật khẩu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Đăng nhập bằng email/mật khẩu, xác định role (CUSTOMER hoặc employee role) và trả JWT.
     */
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Mật khẩu không đúng");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị khóa");
        }

        String role;
        Long customerId = null;
        Long employeeId = null;

        Customer customer = customerRepository.findByUser_UserId(user.getUserId()).orElse(null);
        if (customer != null) {
            role = "CUSTOMER";
            customerId = customer.getCustomerId();
        } else {
            Employee employee = employeeRepository.findByUserId(user.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không hợp lệ"));
            role = employee.getRole();
            employeeId = employee.getEmployeeId();
        }

        String token = jwtTokenProvider.generateToken(user.getUserId(), role);

        log.info("Đăng nhập thành công, userId={}, role={}", user.getUserId(), role);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role)
                .customerId(customerId)
                .employeeId(employeeId)
                .build();
    }

    /**
     * Đăng nhập bằng Google qua Firebase ID Token.
     * Verify token → tìm hoặc tạo user/customer → trả JWT hệ thống.
     */
    @Transactional
    public LoginResponse loginWithGoogle(FirebaseLoginRequest request) {
        try {
            // Bước 1: Xác thực Firebase ID Token từ FE
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            String email = decodedToken.getEmail();
            String name = decodedToken.getName();

            if (email == null || email.isBlank()) {
                throw new BadCredentialsException("Email không có trong token Firebase");
            }

            // Bước 2: Tìm user theo email trong DB
            User user = userRepository.findByEmail(email).orElse(null);

            // Bước 3: Nếu chưa có user → tự động tạo User + Customer mới
            if (user == null) {
                user = new User();
                user.setEmail(email);
                user.setFullName(name != null ? name : email);
                user.setStatus("ACTIVE");
                // Mật khẩu ngẫu nhiên vì đăng nhập qua Google, không dùng password
                user.setPasswordHash(UUID.randomUUID().toString());
                user = userRepository.save(user);

                Customer newCustomer = new Customer();
                newCustomer.setUser(user);
                customerRepository.save(newCustomer);
            }

            if (!"ACTIVE".equals(user.getStatus())) {
                throw new RuntimeException("Tài khoản đã bị khóa");
            }

            // Bước 4: Xác định role – tìm trong bảng customers
            String role;
            Long customerId = null;
            Long employeeId = null;

            Customer customer = customerRepository.findByUser_UserId(user.getUserId()).orElse(null);
            if (customer != null) {
                role = "CUSTOMER";
                customerId = customer.getCustomerId();
            } else {
                Employee employee = employeeRepository.findByUserId(user.getUserId())
                        .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không hợp lệ"));
                role = employee.getRole();
                employeeId = employee.getEmployeeId();
            }

            // Bước 5: Tạo JWT token hệ thống và trả về LoginResponse
            String token = jwtTokenProvider.generateToken(user.getUserId(), role);

            log.info("Đăng nhập Google thành công, userId={}, role={}", user.getUserId(), role);

            return LoginResponse.builder()
                    .token(token)
                    .tokenType("Bearer")
                    .userId(user.getUserId())
                    .fullName(user.getFullName())
                    .email(user.getEmail())
                    .role(role)
                    .customerId(customerId)
                    .employeeId(employeeId)
                    .build();
        } catch (FirebaseAuthException ex) {
            throw new BadCredentialsException("Token Firebase không hợp lệ");
        }
    }
}
