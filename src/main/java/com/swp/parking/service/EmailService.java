package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, "Smart Parking");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu - Smart Parking");
            helper.setText(buildOtpEmailContent(otp), false);

            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage(), e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
        }
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
