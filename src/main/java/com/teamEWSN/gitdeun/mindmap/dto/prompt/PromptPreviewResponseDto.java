package com.teamEWSN.gitdeun.mindmap.dto.prompt;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PromptPreviewResponseDto {
    private Long historyId;
    private String prompt;
    private String summary;

    // TODO: 프롬프트 미리보기용 맵 데이터
    private MindmapGraphResponseDto mindmapGraph;

    private LocalDateTime createdAt;
    private Boolean applied;        // 현재 적용 상태
}