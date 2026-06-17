package com.swp.parking.exception;

import org.springframework.http.HttpStatus;

public class VehicleNotOwnedByUserException extends AppException {

    public VehicleNotOwnedByUserException() {
        super(HttpStatus.FORBIDDEN, "Xe không thuộc sở hữu của bạn");
    }
}
