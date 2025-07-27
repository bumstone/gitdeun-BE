package com.teamEWSN.gitdeun.codereview.entity;

import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmapnode.entity.MindmapNode;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Code_Review")
public class CodeReview extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id", nullable = false)
    private MindmapNode node;

    @Column(name = "ref_id")
    private Long refId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @ColumnDefault("'PENDING'")
    private CodeReviewStatus status;

    @Column(name = "comment_cnt")
    @ColumnDefault("0")
    private Integer commentCount;

    @Column(name = "unresolved_thread_cnt")
    @ColumnDefault("0")
    private Integer unresolvedThreadCount;

}