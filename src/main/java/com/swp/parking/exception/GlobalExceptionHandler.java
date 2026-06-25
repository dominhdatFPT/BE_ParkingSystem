package com.swp.parking.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(AppException ex) {
        log.warn("AppException [{}]: {}", ex.getStatus(), ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatus().value());
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(VehicleAlreadyHasActiveSubscriptionException.class)
    public ResponseEntity<Map<String, Object>> handleVehicleAlreadyHasActiveSubscription(VehicleAlreadyHasActiveSubscriptionException ex) {
        log.warn("VehicleAlreadyHasActiveSubscriptionException: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatus().value());
        body.put("message", ex.getMessage());
        body.put("currentSubscriptionEndDate", ex.getCurrentSubscriptionEndDate());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(VehicleNotOwnedByUserException.class)
    public ResponseEntity<Map<String, Object>> handleVehicleNotOwnedByUser(VehicleNotOwnedByUserException ex) {
        log.warn("VehicleNotOwnedByUserException: {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("status", ex.getStatus().value());
        body.put("message", ex.getMessage());
        body.put("timestamp", LocalDateTime.now());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(FeePackageNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleFeePackageNotFound(FeePackageNotFoundException ex) {
        log.warn("FeePackageNotFoundException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        log.warn("SubscriptionNotFoundException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(InvalidSubscriptionStatusException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidSubscriptionStatus(InvalidSubscriptionStatusException ex) {
        log.warn("InvalidSubscriptionStatusException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFoundException(NotFoundException ex) {
        log.warn("NotFoundException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(AlreadyDeletedException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyDeletedException(AlreadyDeletedException ex) {
        log.warn("AlreadyDeletedException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(DuplicateLicensePlateException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateLicensePlateException(DuplicateLicensePlateException ex) {
        log.warn("DuplicateLicensePlateException: {}", ex.getMessage());
        return handleAppException(ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal server error");
        body.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
