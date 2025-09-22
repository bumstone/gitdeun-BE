package com.teamEWSN.gitdeun.codereview.entity;

import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "code_review")
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
    @Column(name = "node_id")
    private String nodeId; // 마인드맵 그래프의 노드 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_reference_id")
    private CodeReference codeReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodeReviewStatus status;

    @OneToMany(mappedBy = "codeReview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public void changeStatus(CodeReviewStatus status) {
        this.status = status;
    }
}