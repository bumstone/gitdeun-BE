package com.teamEWSN.gitdeun.notification.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.invitation.entity.Invitation;
import com.teamEWSN.gitdeun.notification.dto.NotificationResponseDto;
import com.teamEWSN.gitdeun.notification.dto.UnreadNotificationCountDto;
import com.teamEWSN.gitdeun.notification.entity.Notification;
import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.notification.mapper.NotificationMapper;
import com.teamEWSN.gitdeun.notification.repository.NotificationRepository;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationSseService notificationSseService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationMapper notificationMapper;
    private final JavaMailSender mailSender;

    /**
     * 이메일 초대 알림
     */
    @Transactional
    public void notifyInvitation(Invitation invitation) {
        User invitee = invitation.getInvitee();
        String message = String.format("'%s'님이 '%s' 마인드맵으로 초대했습니다.",
            invitation.getInviter().getName(),
            invitation.getMindmap().getField());
        createAndSendNotification(invitee, NotificationType.INVITE_MINDMAP, message);
    }

    /**
     * 초대 수락 알림 (초대한 사람에게 전송)
     */
    @Transactional
    public void notifyAcceptance(Invitation invitation) {
        User inviter = invitation.getInviter();
        String message = String.format("'%s'님이 '%s' 마인드맵 초대를 수락했습니다.",
            invitation.getInvitee().getName(),
            invitation.getMindmap().getField());
        createAndSendNotification(inviter, NotificationType.INVITE_MINDMAP, message);
    }

    /**
     * 링크 초대 승인 요청 알림 (마인드맵 소유자에게 전송)
     */
    @Transactional
    public void notifyLinkApprovalRequest(Invitation invitation) {
        User owner = invitation.getMindmap().getUser();
        String message = String.format("'%s'님이 링크를 통해 '%s' 마인드맵 참여를 요청했습니다.",
            invitation.getInvitee().getName(),
            invitation.getMindmap().getField());
        createAndSendNotification(owner, NotificationType.INVITE_MINDMAP, message);
    }


    /**
     * 알림 생성 및 발송 (다른 서비스에서 호출)
     */
    @Transactional
    public void createAndSendNotification(User user, NotificationType type, String message) {
        Notification notification = Notification.builder()
            .user(user)
            .notificationType(type)
            .message(message)
            .build();
        notificationRepository.save(notification);

        // 이메일 발송 (비동기 처리)
        sendEmailNotification(user.getEmail(), "[Gitdeun] 새로운 알림이 도착했습니다.", message);

        int unreadCount = notificationRepository.countByUserAndReadFalse(user);
        notificationSseService.sendUnreadCount(user.getId(), unreadCount);
    }

    /**
     * 사용자의 모든 알림 조회 (페이징, 최신순)
     */
    @Transactional(readOnly = true)
    public Page<NotificationResponseDto> getNotifications(Long userId, Pageable pageable) {
        User user = getUserById(userId);
        Page<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return notifications.map(notificationMapper::toResponseDto);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    @Transactional(readOnly = true)
    public UnreadNotificationCountDto getUnreadNotificationCount(Long userId) {
        User user = getUserById(userId);
        int count = notificationRepository.countByUserAndReadFalse(user);
        return new UnreadNotificationCountDto(count);
    }

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        User user = getUserById(userId);
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
            .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // 아직 읽지 않은 알림일 경우에만 처리
        if (!notification.isRead()) {
            notification.markAsRead();

            // 읽음 처리 후, 변경된 '읽지 않은 알림 개수'를 실시간으로 전송
            int unreadCount = notificationRepository.countByUserAndReadFalse(user);
            notificationSseService.sendUnreadCount(user.getId(), unreadCount);
        }
    }

    /**
     * 알림 삭제
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        User user = getUserById(userId);
        Notification notification = notificationRepository.findByIdAndUser(notificationId, user)
            .orElseThrow(() -> new GlobalException(ErrorCode.NOTIFICATION_NOT_FOUND));

        boolean wasUnread = !notification.isRead();

        notificationRepository.delete(notification);

        // 만약 삭제된 알림이 '읽지 않은' 상태인 경우, 개수 조정 후전송
        if (wasUnread) {
            int unreadCount = notificationRepository.countByUserAndReadFalse(user);
            notificationSseService.sendUnreadCount(user.getId(), unreadCount);
        }
    }

    // 사용자 조회 - 편의 메서드
    private User getUserById(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));
    }

    // 이메일 발송 비동기 처리
    @Async
    public void sendEmailNotification(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}