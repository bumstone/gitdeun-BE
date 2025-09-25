package com.teamEWSN.gitdeun.notification.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.notification.dto.NotificationResponseDto;
import com.teamEWSN.gitdeun.notification.dto.UnreadNotificationCountDto;
import com.teamEWSN.gitdeun.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 현재 사용자의 모든 알림 조회
    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationResponseDto> notifications = notificationService.getNotifications(userDetails.getId(), pageable);
        return ResponseEntity.ok(notifications);
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountDto> getUnreadNotificationCount(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        UnreadNotificationCountDto countDto = notificationService.getUnreadNotificationCount(userDetails.getId());
        return ResponseEntity.ok(countDto);
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(notificationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 모든 알림 읽음 처리
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllNotificationsAsRead(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 알림 삭제
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.deleteNotification(notificationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }
}
