package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class AlreadyDeletedException extends AppException {

    public AlreadyDeletedException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
