package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final String PLACEHOLDER_PREFIX = "CHANGE_ME";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public void sendOtpEmail(String toEmail, String otp) {
        validateMailConfiguration();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, "Smart Parking");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu - Smart Parking");
            helper.setText(buildOtpEmailContent(otp), false);

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof MailException
                    || cause instanceof MessagingException
                    || cause instanceof UnsupportedEncodingException) {
                handleMailFailure(toEmail, cause);
            }
            throw e instanceof RuntimeException runtimeException ? runtimeException : new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
        }
    }

    private void validateMailConfiguration() {
        if (!StringUtils.hasText(fromAddress)
                || !StringUtils.hasText(mailPassword)
                || isPlaceholder(fromAddress)
                || isPlaceholder(mailPassword)) {
            log.error("Mail is not configured. Set real MAIL_USERNAME and MAIL_PASSWORD values.");
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Máy chủ email chưa được cấu hình. Vui lòng thử lại sau.");
        }
    }

    private boolean isPlaceholder(String value) {
        return StringUtils.hasText(value)
                && value.trim().toUpperCase().startsWith(PLACEHOLDER_PREFIX);
    }

    private void handleMailFailure(String toEmail, Throwable cause) {
        Throwable rootCause = rootCause(cause);
        log.error("Failed to send OTP email to {}: {}", toEmail, rootCause.getMessage(), cause);

        if (cause instanceof MailAuthenticationException
                || rootCause instanceof jakarta.mail.AuthenticationFailedException) {
            throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Máy chủ email chưa xác thực được. Vui lòng thử lại sau.");
        }

        if (cause instanceof MailSendException) {
            throw new AppException(HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối máy chủ email. Vui lòng thử lại sau.");
        }

        throw new AppException(HttpStatus.SERVICE_UNAVAILABLE,
                "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
    }

    private Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private String buildOtpEmailContent(String otp) {
        return """
                Xin chào,

                Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Smart Parking.

                Mã OTP của bạn là: %s

                Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.

                Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.

                Trân trọng,
                Đội ngũ Smart Parking
                """.formatted(otp);
    }
}
