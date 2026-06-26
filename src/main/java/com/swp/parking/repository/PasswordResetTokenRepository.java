package com.swp.parking.repository;

import com.swp.parking.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByUser_EmailAndOtpCodeAndExpiresAtAfterAndUsedFalse(
            String email, String otpCode, LocalDateTime now);

    Optional<PasswordResetToken> findByUser_EmailAndOtpCodeAndVerifiedTrueAndUsedFalseAndExpiresAtAfter(
            String email, String otpCode, LocalDateTime now);
}
