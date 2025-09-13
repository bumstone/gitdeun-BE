package com.teamEWSN.gitdeun.mindmap.entity;

import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mindmap")
public class Mindmap extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repo repo;

    // owner
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(length = 100, nullable = false)
    private String branch;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "map_data", columnDefinition = "json", nullable = false)
    private String mapData;

    // TODO: 멤버수 제한 기능 (유료?)
    @Builder.Default
    @Column(name = "member_count", nullable = false)
    private Integer memberCount = 1;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder.Default
    @OneToMany(mappedBy = "mindmap", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PromptHistory> promptHistories = new ArrayList<>();


    public void updateMapData(String newMapData) {
        this.mapData = newMapData;
    }

    public void updateTitle(String newTitle) {
        this.title = newTitle;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    // 현재 적용된 프롬프트 히스토리 조회
    public PromptHistory getAppliedPromptHistory() {
        return promptHistories.stream()
            .filter(PromptHistory::getApplied)
            .findFirst()
            .orElse(null);
    }

    // 특정 프롬프트 히스토리의 결과 적용
    public void applyPromptHistory(PromptHistory promptHistory) {
        // 기존 적용 상태 해제
        promptHistories.forEach(PromptHistory::unapply);

        // 새 프롬프트 적용
        promptHistory.applyToMindmap();
        this.mapData = promptHistory.getMapData();
    }


}