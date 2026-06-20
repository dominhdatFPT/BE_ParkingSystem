package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class SubscriptionNotFoundException extends AppException {

    public SubscriptionNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Không tìm thấy đăng ký gói với ID: " + id);
    }
}
