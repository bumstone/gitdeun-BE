package com.teamEWSN.gitdeun.repo.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * GitHub 저장소 정보를 담는 불변 객체
 */
@Builder
@Getter
public class GitHubRepositoryInfo {
    private final String owner;              // 저장소 소유자
    private final String repositoryName;     // 저장소명
    private final String normalizedUrl;      // 정규화된 URL
    private final String originalUrl;        // 원본 입력 URL
    private final boolean isOrganization;    // 조직 저장소 여부
}
