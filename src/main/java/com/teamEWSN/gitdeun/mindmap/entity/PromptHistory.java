package com.teamEWSN.gitdeun.mindmap.entity;

import com.teamEWSN.gitdeun.common.util.CreatedEntity;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "prompt_history")
public class PromptHistory extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String prompt;

    @Column(length = 50)
    private String title;  // 분석 결과 요약 (기록 제목)

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "map_data", columnDefinition = "json", nullable = false)
    private String mapData;  // 해당 프롬프트의 분석 결과 데이터

    @Builder.Default
    @Column(name = "applied", nullable = false)
    private Boolean applied = false;   // 적용 확정 여부

    public void applyToMindmap() {
        this.applied = true;
    }

    public void unapply() {
        this.applied = false;
    }
}