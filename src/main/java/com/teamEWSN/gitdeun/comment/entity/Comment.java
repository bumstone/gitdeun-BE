package com.teamEWSN.gitdeun.comment.entity;

import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@SQLDelete(sql = "UPDATE comment SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "comment")
public class Comment extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "code_review_id", nullable = false)
    private CodeReview codeReview;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Builder.Default
    @OneToMany(mappedBy = "parentComment")
    private List<Comment> replies = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommentAttachment> attachments = new ArrayList<>();

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "emoji_type")
    private EmojiType emojiType;


    public void updateContent(String content) {
        this.content = content;
    }

    public void toggleResolve() {
        if (this.resolvedAt == null) {
            this.resolvedAt = LocalDateTime.now();
        } else {
            this.resolvedAt = null;
        }
    }

    public void updateEmoji(EmojiType emojiType) {
        // 같은 이모지를 다시 선택하면 제거, 다른 이모지를 선택하면 변경
        if (this.emojiType == emojiType) {
            this.emojiType = null;
        } else {
            this.emojiType = emojiType;
        }
    }

    public boolean isResolved() {
        return this.resolvedAt != null;
    }

    public boolean isTopLevel() {
        return this.parentComment == null;
    }

    public boolean isReply() {
        return this.parentComment != null;
    }
}