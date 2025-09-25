package com.teamEWSN.gitdeun.notification.repository;

import com.teamEWSN.gitdeun.notification.entity.Notification;
import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 사용자의 모든 알림을 최신순으로 페이징하여 조회
    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // 사용자의 읽지 않은 알림 개수 조회
    int countByUserAndReadFalse(User user);

    // 특정 알림이 해당 사용자의 소유인지 확인하며 조회
    Optional<Notification> findByIdAndUser(Long id, User user);

    // 특정 사용자의 모든 읽지 않은 알림을 읽음으로 처리하는 메서드
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user = :user AND n.read = false")
    void markAllAsReadByUser(@Param("user") User user);
}