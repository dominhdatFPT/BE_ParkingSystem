package com.swp.parking.repository;

import com.swp.parking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email đăng nhập.
     */
    Optional<User> findByEmail(String email);
}
