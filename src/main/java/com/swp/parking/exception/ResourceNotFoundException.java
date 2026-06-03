package com.swp.parking.exception;

/**
 * Ném khi không tìm thấy tài nguyên yêu cầu (user, xe, đơn đỗ, ...).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
