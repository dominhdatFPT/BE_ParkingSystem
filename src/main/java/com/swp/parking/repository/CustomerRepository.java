package com.swp.parking.repository;

import com.swp.parking.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn dữ liệu bảng customers.
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /**
     * Tìm customer theo userId của tài khoản liên kết.
     */
    Optional<Customer> findByUser_UserId(Long userId);
}
