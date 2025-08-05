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
    private boolean isRead;
    private NotificationType notificationType;
    private LocalDateTime createdAt;
}
