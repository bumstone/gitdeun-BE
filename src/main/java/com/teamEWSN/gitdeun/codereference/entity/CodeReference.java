package com.teamEWSN.gitdeun.codereference.entity;

import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "code_reference")
@SQLDelete(sql = "UPDATE code_reference SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class CodeReference extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Column(name = "file_path", columnDefinition = "TEXT", nullable = false)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Builder.Default
    @OneToMany(mappedBy = "codeReference", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CodeReview> codeReviews = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(String filePath, Integer startLine, Integer endLine) {
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
    }
}