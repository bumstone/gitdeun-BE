package com.teamEWSN.gitdeun.notification.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.invitation.entity.Invitation;
import com.teamEWSN.gitdeun.notification.dto.NotificationCreateDto;
import com.teamEWSN.gitdeun.notification.dto.NotificationResponseDto;
import com.teamEWSN.gitdeun.notification.dto.UnreadNotificationCountDto;
import com.teamEWSN.gitdeun.notification.entity.Notification;
import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.notification.mapper.NotificationMapper;
import com.teamEWSN.gitdeun.notification.repository.NotificationRepository;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.properties.from.name:Gitdeun}")
    private String fromName;

    @Value("${app.front-url}")
    private String frontUrl;

    /**
     * 이메일 초대 알림
     */
    @Transactional
    public void notifyInvitation(Invitation invitation) {
        String webMessage = String.format("'%s'님이 '%s' 마인드맵으로 초대했습니다.",
            invitation.getInviter().getName(),
            invitation.getMindmap().getTitle());

        // 이메일용 초대 링크 및 HTML 메시지 생성
        String invitationLink = frontUrl + "/invitations/" + invitation.getToken();
        String emailMessage = String.format(
            """
            <html>
              <body>
                <h3>%s님이 '%s' 마인드맵으로 초대했습니다.</h3>
                <p>아래 버튼을 클릭하여 초대를 수락하거나 거절할 수 있습니다.</p>
                <a href="%s" style="display: inline-block; padding: 10px 20px; background-color: #007bff; color: #ffffff; text-decoration: none; border-radius: 5px;">
                  초대 확인하기
                </a>
                <p><small>링크는 24시간 동안 유효합니다.</small></p>
              </body>
            </html>
            """,
            invitation.getInviter().getName(),
            invitation.getMindmap().getTitle(),
            invitationLink
        );

        // 웹 알림 메시지와 이메일 메시지를 함께 전달
        createAndSendNotification(invitation, webMessage, emailMessage);
    }

    /**
     * 초대 수락 알림 (초대한 사람에게 전송)
     */
    @Transactional
    public void notifyAcceptance(Invitation invitation) {
        User inviter = invitation.getInviter();
        String message = String.format("'%s'님이 '%s' 마인드맵 초대를 수락했습니다.",
            invitation.getInvitee().getName(),
            invitation.getMindmap().getTitle());

        createAndSendNotification(NotificationCreateDto.actionable(
            inviter,
            NotificationType.ACCEPT_MINDMAP,
            message,
            invitation.getId(),
            invitation.getExpiresAt()
        ));
    }

    /**
     * 링크 초대 승인 요청 알림 (마인드맵 소유자에게 전송)
     */
    @Transactional
    public void notifyLinkApprovalRequest(Invitation invitation) {
        User owner = invitation.getMindmap().getUser();
        String message = String.format("'%s'님이 링크를 통해 '%s' 마인드맵 참여를 요청했습니다.",
            invitation.getInvitee().getName(),
            invitation.getMindmap().getTitle());

        createAndSendNotification(NotificationCreateDto.actionable(
            owner,
            NotificationType.INVITE_MINDMAP,
            message,
            invitation.getId(),
            invitation.getExpiresAt()
        ));
    }

    /**
     * 링크 초대 승인 알림 (링크 초대 요청자에게 전송)
     */
    @Transactional
    public void notifyLinkApproval(Invitation invitation) {
        User invitee = invitation.getInvitee();
        String message = String.format("'%s'님이 '%s' 마인드맵 참여를 승인했습니다.",
            invitation.getMindmap().getUser().getName(),
            invitation.getMindmap().getTitle());

        createAndSendNotification(NotificationCreateDto.actionable(
            invitee,
            NotificationType.ACCEPT_MINDMAP,
            message,
            invitation.getMindmap().getId(),
            invitation.getExpiresAt()
        ));
    }

    /**
     * 링크 초대 거절 알림 (링크 초대 요청자에게 전송)
     */
    @Transactional
    public void notifyLinkRejection(Invitation invitation) {
        String message = String.format("아쉽지만, '%s' 마인드맵 참여 요청이 거절되었습니다.",
            invitation.getMindmap().getTitle());

        createAndSendNotification(NotificationCreateDto.actionable(
            invitation.getInvitee(),
            NotificationType.REJECT_MINDMAP,
            message,
            invitation.getId(),
            invitation.getExpiresAt()
        ));
    }

    // 초대 이메일 알림 생성 및 발송 (HTML 형식)
    @Transactional
    public void createAndSendNotification(Invitation invitation, String webMessage, String emailMessage) {
        // Notification 엔티티 생성 (웹 알림용 메시지 사용)
        Notification notification = Notification.builder()
            .user(invitation.getInvitee())
            .notificationType(NotificationType.INVITE_MINDMAP)
            .message(webMessage)
            .referenceId(invitation.getId())
            .expiresAt(invitation.getExpiresAt())
            .build();
        notificationRepository.save(notification);

        // HTML 형식의 이메일 발송
        sendEmailNotification(invitation.getInvitee().getEmail(), "[Gitdeun] 마인드맵 초대장이 도착했습니다.", emailMessage);

        // 실시간 알림 전송 (웹)
        int unreadCount = notificationRepository.countByUserAndReadFalse(invitation.getInvitee());
        notificationSseService.sendUnreadCount(invitation.getInvitee().getId(), unreadCount);
        notificationSseService.sendNewNotification(invitation.getInvitee().getId(), notificationMapper.toResponseDto(notification));
    }

    /**
     * 알림 생성 및 발송 (공통 호출 - 메세지 전송)
     */
    @Transactional
    public void createAndSendNotification(NotificationCreateDto dto) {
        User user = dto.getUser();
        String message = dto.getMessage();

        Notification notification = Notification.builder()
            .user(user)
            .notificationType(dto.getType())
            .message(message)
            .referenceId(dto.getReferenceId())
            .expiresAt(dto.getExpiresAt())
            .build();
        notificationRepository.save(notification);

        // 이메일 발송 (비동기 처리)
        sendEmailNotification(user.getEmail(), "[Gitdeun] 새로운 알림이 도착했습니다.", message);

        int unreadCount = notificationRepository.countByUserAndReadFalse(user);
        notificationSseService.sendUnreadCount(user.getId(), unreadCount);

        // 새 알림 전송
        notificationSseService.sendNewNotification(dto.getUser().getId(),
            notificationMapper.toResponseDto(notification));
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
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);
            helper.setFrom(fromEmail, fromName); // 이름과 이메일 분리 설정

            mailSender.send(message);
            log.info("Email sent to {} from {}", to, fromName);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
        }
    }
}