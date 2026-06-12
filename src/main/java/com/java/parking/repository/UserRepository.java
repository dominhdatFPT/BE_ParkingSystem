package com.swp.parking.repository;

import com.swp.parking.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query(value = "select e.role from employees e where e.user_id = :userId and e.status = 'ACTIVE' limit 1", nativeQuery = true)
    Optional<String> findActiveEmployeeRoleByUserId(@Param("userId") Long userId);
}
