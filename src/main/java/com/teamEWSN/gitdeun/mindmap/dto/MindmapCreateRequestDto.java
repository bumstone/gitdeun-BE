package com.teamEWSN.gitdeun.mindmap.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MindmapCreateRequestDto {
    private String repoUrl;
    private String prompt;
}
