package com.teamEWSN.gitdeun.mindmapedge.entity;

import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.mindmapnode.entity.MindmapNode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Mindmap_Edge")
public class MindmapEdge extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_node_id", nullable = false)
    private MindmapNode fromNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_node_id", nullable = false)
    private MindmapNode toNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EdgeType type;

    @Column(nullable = false)
    @ColumnDefault("0")
    private BigDecimal strength;

    @Column(name = "arango_key")
    private Long arangoKey;

}