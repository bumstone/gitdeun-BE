package com.teamEWSN.gitdeun.notification.entity;

public enum NotificationType {
    INVITE_MINDMAP,      // 마인드맵 초대
    MENTION_COMMENT,    // 댓글에서 맨션
    APPLICATION_RECEIVED,   // 지원 신청
    APPLICATION_ACCEPTED,   // 신청 수락
    APPLICATION_REJECTED,   // 신청 거절
    SYSTEM_UPDATE;       // 시스템 업데이트(webhook)
}
