package com.teamEWSN.gitdeun.notification.dto;

import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponseDto {
    private Long notificationId;
    private String message;
    private boolean read;
    private NotificationType notificationType;
    private LocalDateTime createdAt;

    private Long referenceId;   // mindmapId, commentId 등등
    private LocalDateTime expiresAt;

    public boolean isActionAvailable() {
        return expiresAt != null && expiresAt.isAfter(LocalDateTime.now());
    }
}
