package com.teamEWSN.gitdeun.notification.entity;

public enum NotificationType {
    INVITE_MINDMAP,      // 마인드맵 초대
    ACCEPT_MINDMAP,     // 초대 수락
    REJECT_MINDMAP,     // 초대 거절
    MENTION_COMMENT,    // 댓글에서 맨션
    APPLICATION_RECEIVED,   // 지원 신청
    APPLICATION_ACCEPTED,   // 지원 수락
    APPLICATION_REJECTED,   // 지원 거절
    APPLICATION_WITHDRAWN_AFTER_ACCEPTANCE,     // 지원 수락 철회

    ANALYSIS_PROMPT,    // 분석 프롬프트
    MINDMAP_CREATE,     // 마인드맵 생성
    MINDMAP_UPDATE,     // 마인드맵 업데이트
    MINDMAP_DELETE,     // 마인드맵 삭제
    SYSTEM_UPDATE       // 시스템 업데이트(webhook)
}
