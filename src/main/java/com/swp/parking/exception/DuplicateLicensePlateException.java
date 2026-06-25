package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class DuplicateLicensePlateException extends AppException {

    public DuplicateLicensePlateException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
