package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class MindmapDetailResponseDto {
    private Long mindmapId;
    private String title;
    private String branch;
    private String mapData; // 핵심 데이터인 마인드맵 JSON
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<PromptHistoryResponseDto> promptHistories;
    private PromptHistoryResponseDto appliedPromptHistory;  // 현재 적용된 프롬프트
}
