package com.swp.parking.service;

import com.swp.parking.exception.AppException;
import com.swp.parking.model.Card;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.CardStatus;
import com.swp.parking.repository.CardRepository;
import com.swp.parking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    public Card ensureActiveUserCard(Long userId) {
        User user = findUserById(userId);
        Card card = cardRepository.findByUserId(userId)
                .orElseGet(() -> cardRepository.save(buildNewCard(user, null)));

        if (card.getStatus() != CardStatus.ACTIVE) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Your internal card is not active");
        }

        if (card.getExpiredAt() != null && card.getExpiredAt().isBefore(LocalDateTime.now())) {
            card.setStatus(CardStatus.EXPIRED);
            cardRepository.save(card);
            throw new AppException(HttpStatus.BAD_REQUEST, "Your internal card is expired");
        }

        return card;
    }

    private Card buildNewCard(User user, LocalDateTime expiredAt) {
        return Card.builder()
                .user(user)
                .cardCode(generateCardCode(user.getId()))
                .status(CardStatus.ACTIVE)
                .issuedAt(LocalDateTime.now())
                .expiredAt(expiredAt)
                .build();
    }

    private String generateCardCode(Long userId) {
        String baseCode = String.format("CARD-%06d", userId);
        if (!cardRepository.existsByCardCode(baseCode)) {
            return baseCode;
        }

        int suffix = 1;
        String candidate;
        do {
            candidate = String.format("%s-%02d", baseCode, suffix++);
        } while (cardRepository.existsByCardCode(candidate));
        return candidate;
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
