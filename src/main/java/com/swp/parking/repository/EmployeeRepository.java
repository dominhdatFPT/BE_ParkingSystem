package com.swp.parking.repository;

import com.swp.parking.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn dữ liệu bảng employees.
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * Tìm employee theo userId (trường Long thuần, không phải quan hệ JPA).
     */
    @Query("SELECT e FROM Employee e WHERE e.userId = :userId")
    Optional<Employee> findByUserId(@Param("userId") Long userId);
}
