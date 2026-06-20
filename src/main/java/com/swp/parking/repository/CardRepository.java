package com.swp.parking.repository;

import com.swp.parking.model.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByUserId(Long userId);

    boolean existsByCardCode(String cardCode);
}
