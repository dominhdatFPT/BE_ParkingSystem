package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class InvalidRoleException extends AppException {

    public InvalidRoleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
