package com.swp.parking.service;

import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

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

    public String register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email đã được đăng ký");
        }
        if (userRepository.findByPhone(request.getPhone()).isPresent()) {
            throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        try {
            User savedUser = userRepository.saveAndFlush(user);

            Customer customer = new Customer();
            customer.setUser(savedUser);
            customer.setAddress(request.getAddress());
            customer.setCreatedAt(LocalDateTime.now());
            customerRepository.save(customer);

            log.info("Đăng ký thành công, userId={}, email={}", savedUser.getUserId(), savedUser.getEmail());
            return "Đăng ký tài khoản thành công";
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("Dữ liệu đăng ký không hợp lệ hoặc đã tồn tại");
        }
    }
}
