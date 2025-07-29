package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class MindmapDetailResponseDto {
    private Long mindmapId;
    private String field; // 제목 ("개발용", "확인용(n)" 등)
    private MindmapType type;
    private String branch;
    private String prompt;
    private String mapData; // 핵심 데이터인 마인드맵 JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
