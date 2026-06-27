package com.swp.parking.repository;

import com.swp.parking.dto.response.EmployeeResponse;
import com.swp.parking.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("""
            SELECT new com.swp.parking.dto.response.EmployeeResponse(
                e.id, u.id, u.fullName, u.email, u.phone, u.avatarUrl,
                e.employeeCode, e.role, e.status, e.createdAt
            )
              FROM Employee e, User u
             WHERE u.id = e.userId
               AND (:role IS NULL OR e.role = :role)
               AND (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(e.employeeCode, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<EmployeeResponse> searchAccountEmployees(
            @Param("role") String role,
            @Param("keyword") String keyword,
            Pageable pageable);

    Optional<Employee> findByUserId(Long userId);

    long countByEmployeeCodeStartingWith(String prefix);
}
