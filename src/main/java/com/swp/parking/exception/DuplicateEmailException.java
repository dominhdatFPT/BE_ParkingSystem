package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends AppException {

    public DuplicateEmailException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
