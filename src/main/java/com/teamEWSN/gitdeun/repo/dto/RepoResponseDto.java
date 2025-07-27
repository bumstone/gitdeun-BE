package com.teamEWSN.gitdeun.repo.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RepoResponseDto {

    private final Long repoId;
    private final String githubRepoUrl;
    private final String defaultBranch;
    private final String language;
    private final String description;
    private final LocalDateTime githubLastUpdatedAt;
    private final LocalDateTime lastSyncedAt;

    @Setter // Mapper의 @AfterMapping에서 이 값을 설정
    private boolean updateNeeded; // 재동기화 필요 여부 플래그
}
