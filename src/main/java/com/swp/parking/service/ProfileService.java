package com.swp.parking.service;

import com.swp.parking.dto.request.ChangePasswordRequest;
import com.swp.parking.exception.AppException;
import com.swp.parking.model.User;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void verifyCurrentPassword(Long userId, String password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không đúng");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}
