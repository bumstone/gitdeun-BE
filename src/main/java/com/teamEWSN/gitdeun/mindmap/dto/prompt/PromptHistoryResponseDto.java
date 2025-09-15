package com.teamEWSN.gitdeun.mindmap.dto.prompt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class PromptHistoryResponseDto {
    private Long historyId;
    private String prompt;
    private String title;
    private Boolean applied;
    private LocalDateTime createdAt;
}
