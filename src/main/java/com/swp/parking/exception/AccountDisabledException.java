package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AppException {

    public AccountDisabledException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
