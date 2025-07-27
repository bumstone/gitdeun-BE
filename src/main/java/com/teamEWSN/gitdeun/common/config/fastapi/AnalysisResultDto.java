package com.teamEWSN.gitdeun.common.config.fastapi;

import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AnalysisResultDto {
    // FastAPI가 반환하는 Repo 관련 정보
    private String defaultBranch;
    private String language;
    private String description;
    private LocalDateTime githubLastUpdatedAt;

    // FastAPI가 반환하는 Mindmap 관련 정보
    private String mapData;
    private MindmapType type;
    private String prompt;
    // TODO: FastAPI 응답에 맞춰 필드 정의
}
