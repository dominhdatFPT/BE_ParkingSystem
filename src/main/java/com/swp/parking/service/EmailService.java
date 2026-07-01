package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import jakarta.annotation.PreDestroy;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final ExecutorService mailExecutor = Executors.newFixedThreadPool(2);

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.mail.otp-send-timeout-ms:10000}")
    private long otpSendTimeoutMs;

    public void sendOtpEmail(String toEmail, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress, "Smart Parking");
            helper.setTo(toEmail);
            helper.setSubject("Mã xác nhận đặt lại mật khẩu - Smart Parking");
            helper.setText(buildOtpEmailContent(otp), false);

            CompletableFuture<Void> sendTask = CompletableFuture.runAsync(
                    () -> mailSender.send(message), mailExecutor);
            sendTask.get(otpSendTimeoutMs, TimeUnit.MILLISECONDS);
            log.info("OTP email sent to {}", toEmail);
        } catch (TimeoutException e) {
            log.error("Timed out sending OTP email to {} after {} ms", toEmail, otpSendTimeoutMs);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Gửi mã OTP qua email quá lâu. Vui lòng thử lại sau.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof MailException
                    || cause instanceof MessagingException
                    || cause instanceof UnsupportedEncodingException) {
                log.error("Failed to send OTP email to {}: {}", toEmail, cause.getMessage(), cause);
                throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
            }
            throw e instanceof RuntimeException runtimeException ? runtimeException : new AppException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể gửi mã OTP qua email. Vui lòng thử lại sau.");
        }
    }

    @PreDestroy
    public void shutdownMailExecutor() {
        mailExecutor.shutdownNow();
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
