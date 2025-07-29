package com.teamEWSN.gitdeun.mindmap.entity;

import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.repo.entity.Repo;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private MindmapType type;

    @Column(name = "Field", length = 255, nullable = false)
    private String field;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "map_data", columnDefinition = "json", nullable = false)
    private String mapData;

}