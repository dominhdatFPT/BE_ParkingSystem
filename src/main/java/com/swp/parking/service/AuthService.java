package com.swp.parking.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.swp.parking.dto.request.FirebaseLoginRequest;
import com.swp.parking.dto.request.ForgotPasswordRequest;
import com.swp.parking.dto.request.LoginRequest;
import com.swp.parking.dto.request.RegisterRequest;
import com.swp.parking.dto.request.ResetPasswordRequest;
import com.swp.parking.dto.request.VerifyOtpRequest;
import com.swp.parking.dto.response.LoginResponse;
import com.swp.parking.entity.Customer;
import com.swp.parking.entity.Employee;
import com.swp.parking.entity.PasswordResetToken;
import com.swp.parking.entity.User;
import com.swp.parking.exception.ResourceNotFoundException;
import com.swp.parking.repository.CustomerRepository;
import com.swp.parking.repository.EmployeeRepository;
import com.swp.parking.repository.PasswordResetTokenRepository;
import com.swp.parking.repository.UserRepository;
import com.swp.parking.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
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

    public Long requestPasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Email không tồn tại"));

        String otpCode = generateOtpCode();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setOtpCode(otpCode);
        token.setVerified(false);
        token.setUsed(false);
        token.setCreatedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        PasswordResetToken savedToken = passwordResetTokenRepository.save(token);

        log.info("OTP quên mật khẩu cho email={} là {} (thời hạn 5 phút)", user.getEmail(), otpCode);

        return savedToken.getTokenId();
    }

    public String verifyPasswordResetOtp(VerifyOtpRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenIdAndOtpCodeAndExpiresAtAfterAndUsedFalse(request.getRequestId(), request.getOtp(), LocalDateTime.now())
                .orElseThrow(() -> new BadCredentialsException("OTP không đúng hoặc đã hết hạn"));

        if (Boolean.TRUE.equals(token.getVerified())) {
            throw new IllegalArgumentException("OTP đã được xác thực");
        }

        token.setVerified(true);
        token.setResetToken(UUID.randomUUID().toString());
        token.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        passwordResetTokenRepository.save(token);

        log.info("Đã xác thực OTP quên mật khẩu cho email={}", token.getUser().getEmail());
        return token.getResetToken();
    }

    public String resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository
                .findByResetTokenAndVerifiedTrueAndUsedFalseAndExpiresAtAfter(request.getResetToken(), LocalDateTime.now())
                .orElseThrow(() -> new BadCredentialsException("Yêu cầu đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"));

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        log.info("Đặt lại mật khẩu thành công cho email={}", user.getEmail());
        return "Đặt lại mật khẩu thành công";
    }

    private String generateOtpCode() {
        int otp = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return String.valueOf(otp);
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
