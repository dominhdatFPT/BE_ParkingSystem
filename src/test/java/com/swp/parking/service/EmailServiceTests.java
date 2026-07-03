package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class EmailServiceTests {

    @Mock JavaMailSender mailSender;

    @Test
    void placeholderMailCredentialsAreTreatedAsMissingConfiguration() {
        EmailService service = new EmailService(mailSender);
        ReflectionTestUtils.setField(service, "fromAddress", "CHANGE_ME_YOUR_GMAIL");
        ReflectionTestUtils.setField(service, "mailPassword", "CHANGE_ME_YOUR_GMAIL_APP_PASSWORD");

        AppException exception = assertThrows(AppException.class,
                () -> service.sendOtpEmail("user@example.com", "123456"));

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatus());
        assertEquals("Máy chủ email chưa được cấu hình. Vui lòng thử lại sau.", exception.getMessage());
    }
}
