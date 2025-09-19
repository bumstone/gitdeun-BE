package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalysisResultDto {
    // Repo 관련 정보
    private String defaultBranch;
    private LocalDateTime lastCommit;

    // Mindmap 관련 정보
    private String title;   // 프롬프트 및 mindmap 정보 요약
    private String errorMessage;    // 실패 시 전달될 에러메세지
    // TODO: FastAPI 응답에 맞춰 필드 정의
}
