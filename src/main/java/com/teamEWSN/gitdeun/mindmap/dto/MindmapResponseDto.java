package com.teamEWSN.gitdeun.mindmap.dto;

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
    private String title;
    private String prompt;
    private LocalDateTime createdAt;
}