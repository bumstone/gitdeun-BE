package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AnalysisResultDto {
    // Repo 관련 정보
    private String defaultBranch;
    private LocalDateTime githubLastUpdatedAt;

    // Mindmap 관련 정보
    private String mapData;     // JSON 형태의 마인드맵 데이터
    private String title;
    private String errorMessage;    // 실패 시 전달될 에러메세지
    private String analysisSummary;
    // TODO: FastAPI 응답에 맞춰 필드 정의
}
