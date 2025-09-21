package com.teamEWSN.gitdeun.notification.entity;

public enum NotificationType {
    INVITE_MINDMAP,      // 마인드맵 초대
    ACCEPT_MINDMAP,     // 초대 수락
    REJECT_MINDMAP,
    MENTION_COMMENT,    // 댓글에서 맨션
    APPLICATION_RECEIVED,   // 지원 신청
    APPLICATION_ACCEPTED,   // 지원 수락
    APPLICATION_REJECTED,   // 지원 거절
    APPLICATION_WITHDRAWN_AFTER_ACCEPTANCE,     // 지원 수락 철회
    SYSTEM_UPDATE       // 시스템 업데이트(webhook)
}
