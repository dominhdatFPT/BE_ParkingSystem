package com.swp.parking.repository;

import com.swp.parking.dto.response.AccountUserResponse;
import com.swp.parking.model.User;
import com.swp.parking.model.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountUserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT new com.swp.parking.dto.response.AccountUserResponse(
                u.id, u.fullName, u.email, u.phone, u.avatarUrl, u.status, u.role, u.createdAt
            )
              FROM User u
             WHERE u.role = :role
               AND (:status IS NULL OR u.status = :status)
               AND (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AccountUserResponse> searchAccountUsers(
            @Param("role") UserRole role,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable);

    @Query("""
            SELECT new com.swp.parking.dto.response.AccountUserResponse(
                u.id, u.fullName, u.email, u.phone, u.avatarUrl, u.status, u.role, u.createdAt
            )
              FROM User u
             WHERE u.role IN :roles
               AND (:role IS NULL OR u.role = :role)
               AND (:keyword IS NULL OR :keyword = ''
                    OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AccountUserResponse> searchAccountStaffs(
            @Param("roles") List<UserRole> roles,
            @Param("role") UserRole role,
            @Param("keyword") String keyword,
            Pageable pageable);
}
