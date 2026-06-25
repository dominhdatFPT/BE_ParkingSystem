package com.swp.parking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendOtpEmail(String toEmail, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Mã xác nhận đặt lại mật khẩu - Smart Parking");
            message.setText(
                    "Xin chào,\n\n"
                    + "Bạn vừa yêu cầu đặt lại mật khẩu cho tài khoản Smart Parking.\n\n"
                    + "Mã OTP của bạn là: " + otp + "\n\n"
                    + "Mã có hiệu lực trong 5 phút. Vui lòng không chia sẻ mã này với bất kỳ ai.\n\n"
                    + "Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email này.\n\n"
                    + "Trân trọng,\nĐội ngũ Smart Parking"
            );
            mailSender.send(message);
            log.info("OTP email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }
}
