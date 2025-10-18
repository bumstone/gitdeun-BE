package com.teamEWSN.gitdeun.codereview.entity;

import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "code_review")
@SQLDelete(sql = "UPDATE code_review SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CodeReview extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    // 리뷰 대상: 노드 ID 또는 코드 참조
    @Column(name = "node_key")
    private String nodeKey; // 마인드맵 그래프의 노드 key

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_reference_id")
    private CodeReference codeReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodeReviewStatus status;

    @Builder.Default
    @OneToMany(mappedBy = "codeReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void changeStatus(CodeReviewStatus status) {
        this.status = status;
    }
}