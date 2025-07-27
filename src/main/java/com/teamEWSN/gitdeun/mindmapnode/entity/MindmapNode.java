package com.teamEWSN.gitdeun.mindmapnode.entity;

import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Mindmap_Node")
public class MindmapNode extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    @Column(length = 100, nullable = false)
    private String label;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String path;

    @Column(nullable = false)
    @ColumnDefault("1")
    private Integer depth;

    @Column(name = "arango_key", length = 64)
    private String arangoKey;

    @Column(name = "Importance", nullable = false)
    @ColumnDefault("0")
    private Double importance;
}