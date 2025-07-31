package com.teamEWSN.gitdeun.visithistory.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.teamEWSN.gitdeun.common.util.CreatedEntity;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "pinned_history", indexes = {
    @Index(name = "idx_pinnedHistory_user_visit_history", columnList = "user_id, visit_history_id", unique = true), // 주요 조회 조건 및 중복 방지
    @Index(name = "idx_pinnedHistory_visit_history_id", columnList = "visit_history_id") // 방문 기록 기준 핀 고정 목록 조회
})
public class PinnedHistory extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_history_id", nullable = false)
    private VisitHistory visitHistory;

    @Builder
    public PinnedHistory(User user, VisitHistory visitHistory) {
        this.user = user;
        this.visitHistory = visitHistory;
    }
}
