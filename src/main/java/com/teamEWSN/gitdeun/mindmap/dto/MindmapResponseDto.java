package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MindmapResponseDto {
    private Long mindmapId;
    private Long repoId;
    private MindmapType type;
    private String field;
    private LocalDateTime createdAt;
}