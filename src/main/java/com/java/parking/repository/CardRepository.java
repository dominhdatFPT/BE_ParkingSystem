package com.swp.parking.repository;

import com.swp.parking.model.Card;
import com.swp.parking.model.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByCardCode(String cardCode);

    List<Card> findByStatusOrderByCreatedAtDesc(CardStatus status);
}
