package com.swp.parking.service;

import com.swp.parking.dto.request.CardCreateRequest;
import com.swp.parking.dto.request.CardStatusUpdateRequest;
import com.swp.parking.dto.response.CardResponse;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CardResponse> getAllCards(CardStatus status) {
        List<Card> cards = status == null
                ? cardRepository.findAll()
                : cardRepository.findByStatusOrderByCreatedAtDesc(status);

        return cards.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardResponse getCardById(Long cardId) {
        return mapToResponse(findCardById(cardId));
    }

    public CardResponse getOrCreateMyCard(Long userId) {
        User user = findUserById(userId);
        Card card = cardRepository.findByUserId(userId)
                .orElseGet(() -> cardRepository.save(buildNewCard(user, null)));

        if (card.getExpiredAt() != null
                && card.getExpiredAt().isBefore(LocalDateTime.now())
                && card.getStatus() == CardStatus.ACTIVE) {
            card.setStatus(CardStatus.EXPIRED);
            card = cardRepository.save(card);
        }

        return mapToResponse(card);
    }

    public CardResponse createCard(CardCreateRequest request) {
        User user = findUserById(request.getUserId());
        if (cardRepository.existsByUserId(user.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "This user already has an internal card");
        }

        Card card = buildNewCard(user, request.getExpiredAt());
        return mapToResponse(cardRepository.save(card));
    }

    public CardResponse updateStatus(Long cardId, CardStatusUpdateRequest request) {
        Card card = findCardById(cardId);
        card.setStatus(request.getStatus());
        return mapToResponse(cardRepository.save(card));
    }

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

    private Card findCardById(Long cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Card not found"));
    }

    private CardResponse mapToResponse(Card card) {
        User user = card.getUser();
        return CardResponse.builder()
                .id(card.getId())
                .userId(user.getId())
                .userFullName(user.getFullName())
                .userEmail(user.getEmail())
                .cardCode(card.getCardCode())
                .status(card.getStatus())
                .issuedAt(card.getIssuedAt())
                .expiredAt(card.getExpiredAt())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }
}
