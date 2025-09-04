package com.teamEWSN.gitdeun.notification.dto;

import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationCreateDto {
    private User user;
    private NotificationType type;
    private String message;
    private Long referenceId;
    private LocalDateTime expiresAt;

    // 알림 유형별 기본 만료 기간 설정
    private static LocalDateTime getDefaultExpiryDate(NotificationType type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case MENTION_COMMENT -> now.plusDays(30);       // 댓글 알림은 30일
            default -> now.plusDays(30);            // 기본 30일
        };
    }

    // 기본 만료 기간이 적용된 알림 생성
    public static NotificationCreateDto simple(User user, NotificationType type, String message) {
        return NotificationCreateDto.builder()
            .user(user)
            .type(type)
            .message(message)
            .expiresAt(getDefaultExpiryDate(type))
            .build();
    }

    // 만료 기간과 참조 ID가 지정된 알림 생성
    public static NotificationCreateDto actionable(User user, NotificationType type,
                                                   String message, Long referenceId,
                                                   LocalDateTime expiresAt) {
        return NotificationCreateDto.builder()
            .user(user)
            .type(type)
            .message(message)
            .referenceId(referenceId)
            .expiresAt(expiresAt != null ? expiresAt : getDefaultExpiryDate(type))
            .build();
    }
}
