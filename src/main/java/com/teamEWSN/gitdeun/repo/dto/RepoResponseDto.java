package com.teamEWSN.gitdeun.repo.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RepoResponseDto {

    private final Long repoId;
    private final String githubRepoUrl;
    private final String language;
    private final String defaultBranch;
    private final LocalDateTime lastCommit;

}
