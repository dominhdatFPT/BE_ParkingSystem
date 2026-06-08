package com.swp.parking.repository;

import com.swp.parking.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByTokenIdAndOtpCodeAndExpiresAtAfterAndUsedFalse(Long tokenId,
                                                                                     String otpCode,
                                                                                     LocalDateTime now);

    Optional<PasswordResetToken> findByResetTokenAndVerifiedTrueAndUsedFalseAndExpiresAtAfter(String resetToken,
                                                                                              LocalDateTime now);
}
