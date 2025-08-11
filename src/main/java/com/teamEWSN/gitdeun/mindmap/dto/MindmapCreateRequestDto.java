package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MindmapCreateRequestDto {
    private String repoUrl;
    private String prompt;  // Optional, 'DEV' 타입일 때 사용자가 입력하는 명령어
    private MindmapType type;

    private String field;    // Optional, 'CHECK' 타입일 때 사용자가 입력하는 제목
}
