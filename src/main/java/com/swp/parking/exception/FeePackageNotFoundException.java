package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class FeePackageNotFoundException extends AppException {

    public FeePackageNotFoundException(Long id) {
        super(HttpStatus.NOT_FOUND, "Không tìm thấy gói phí với ID: " + id);
    }
}
