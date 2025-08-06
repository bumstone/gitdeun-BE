package com.teamEWSN.gitdeun.notification.entity;

import com.teamEWSN.gitdeun.common.util.CreatedEntity;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification", indexes = {
    // 사용자별 알림을 최신순으로 조회하는 쿼리 최적화용 인덱스
    @Index(name = "idx_notification_user_created_at", columnList = "user_id, createdAt DESC"),
    // 사용자의 읽지 않은 알림을 조회하는 쿼리 최적화용 인덱스
    @Index(name = "idx_notification_user_read", columnList = "user_id, read")
})
public class Notification extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알림을 받는 사용자

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message; // 알림 메시지 내용

    @Column(nullable = false)
    private boolean read; // 읽음 여부 (기본값: false)

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType; // 알림 종류

    @Builder
    public Notification(User user, String message, NotificationType notificationType) {
        this.user = user;
        this.message = message;
        this.notificationType = notificationType;
        this.read = false;
    }

    // 읽음 처리
    public void markAsRead() {
        this.read = true;
    }
}
