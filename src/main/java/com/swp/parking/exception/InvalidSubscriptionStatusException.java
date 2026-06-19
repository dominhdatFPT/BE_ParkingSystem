package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class InvalidSubscriptionStatusException extends AppException {

    public InvalidSubscriptionStatusException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
