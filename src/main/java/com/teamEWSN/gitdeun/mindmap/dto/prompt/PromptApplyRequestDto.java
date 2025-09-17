package com.teamEWSN.gitdeun.mindmap.dto.prompt;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PromptApplyRequestDto {
    private Long historyId;  // 적용할 히스토리 ID
}