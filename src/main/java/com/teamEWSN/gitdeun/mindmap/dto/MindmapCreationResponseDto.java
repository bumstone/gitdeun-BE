package com.teamEWSN.gitdeun.mindmap.dto;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class MindmapCreationResponseDto {
    private String processId;
    private String message;
}
