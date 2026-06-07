package com.swp.parking.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Bắt và chuyển đổi exception toàn cục thành HTTP response chuẩn.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Xử lý khi không tìm thấy tài nguyên – trả 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Trả về 400 khi sai mật khẩu.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex) {
        // Trả về 400 khi sai mật khẩu
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Trả về 403 khi tài khoản bị khóa.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = "Dữ liệu trùng lặp hoặc không hợp lệ";
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (detail != null) {
            if (detail.contains("users_phone_key")) {
                message = "Số điện thoại đã được sử dụng";
            } else if (detail.contains("users_email_key")) {
                message = "Email đã được đăng ký";
            }
        }
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystem(TransactionSystemException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;

        Throwable cause = ex;
        while (cause != null) {
            String text = cause.getMessage();
            if (text != null && text.contains("duplicate key value violates unique constraint")) {
                if (text.contains("users_phone_key")) {
                    message = "Số điện thoại đã được sử dụng";
                } else if (text.contains("users_email_key")) {
                    message = "Email đã được đăng ký";
                } else {
                    message = "Dữ liệu trùng lặp hoặc không hợp lệ";
                }
                return ResponseEntity.status(status).body(new ErrorResponse(status.value(), message, LocalDateTime.now()));
            }
            cause = cause.getCause();
        }

        return handleRuntime(ex);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.FORBIDDEN;

        Throwable cause = ex;
        while (cause != null) {
            String text = cause.getMessage();
            if (text != null && text.contains("duplicate key value violates unique constraint")) {
                status = HttpStatus.BAD_REQUEST;
                if (text.contains("users_phone_key")) {
                    message = "Số điện thoại đã được sử dụng";
                } else if (text.contains("users_email_key")) {
                    message = "Email đã được đăng ký";
                } else {
                    message = "Dữ liệu trùng lặp hoặc không hợp lệ";
                }
                break;
            }
            cause = cause.getCause();
        }

        ErrorResponse error = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Xử lý mọi lỗi không lường trước – trả 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
