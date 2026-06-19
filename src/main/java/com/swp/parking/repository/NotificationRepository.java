package com.swp.parking.repository;

import com.swp.parking.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByIsActiveTrueOrderByPublishedAtDesc(Pageable pageable);
}
