package com.teamEWSN.gitdeun.mindmap.dto.request;

import com.teamEWSN.gitdeun.repo.dto.GitHubRepositoryInfo;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 검증 완료된 마인드맵 생성 요청 정보
 */
@Builder
@Getter
public class ValidatedMindmapRequest {
    private final GitHubRepositoryInfo repositoryInfo;
    private final String processedPrompt;
    private final Long userId;
    private final LocalDateTime validatedAt;

    /**
     * FastAPI 호출용 정규화된 URL 반환
     */
    public String getNormalizedRepositoryUrl() {
        return repositoryInfo.getNormalizedUrl();
    }

    /**
     * 프롬프트 기반 분석 여부 확인
     */
    public boolean hasPrompt() {
        return StringUtils.hasText(processedPrompt);
    }

    /**
     * 로깅용 간단 정보 반환
     */
    public String getLogSummary() {
        return String.format("%s/%s %s",
            repositoryInfo.getOwner(),
            repositoryInfo.getRepositoryName(),
            hasPrompt() ? "(프롬프트 있음)" : "(기본 분석)");
    }
}
