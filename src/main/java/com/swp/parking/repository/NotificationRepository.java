package com.swp.parking.repository;

import com.swp.parking.model.Notification;
import com.swp.parking.model.enums.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByIsActiveTrueOrderByPublishedAtDesc(Pageable pageable);

    List<Notification> findByIsActiveTrueAndStatusOrderByPublishedAtDesc(NotificationStatus status);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.isActive = true AND n.status = :status
              AND (n.recipientUserId = :userId
                   OR (n.recipientUserId IS NULL AND n.recipientTarget <> com.swp.parking.model.enums.NotificationRecipientTarget.SPECIFIC_USER))
            ORDER BY n.publishedAt DESC
            """)
    List<Notification> findVisibleForUser(@Param("userId") Long userId,
                                          @Param("status") NotificationStatus status);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
