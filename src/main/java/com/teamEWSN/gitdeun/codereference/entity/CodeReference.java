package com.teamEWSN.gitdeun.codereference.entity;

import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "code_reference")
public class CodeReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    @Column(name = "file_path", columnDefinition = "TEXT", nullable = false)
    private String filePath;

    @Column(name = "start_line", length = 255)
    private String startLine;

    @Column(name = "end_line", length = 255)
    private String endLine;
}