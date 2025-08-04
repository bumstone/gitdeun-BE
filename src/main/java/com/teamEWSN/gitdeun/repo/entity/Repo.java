package com.teamEWSN.gitdeun.repo.entity;

import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "repo")
public class Repo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "github_repo_url", length = 512, nullable = false, unique = true)
    private String githubRepoUrl;

//    @Column(name="repo_name", length = 255, nullable = false)
//    private String repoName;
//
//    @Column(length = 100)
//    private String language;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;   // 기본 브랜치

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    @Column(name = "github_last_updated_at")
    private LocalDateTime githubLastUpdatedAt; // GitHub 브랜치 최신 커밋 시간 (commit.committer.date)


    @Builder
    public Repo(String githubRepoUrl, String defaultBranch, String description, LocalDateTime githubLastUpdatedAt) {
        this.githubRepoUrl = githubRepoUrl;
        this.defaultBranch = defaultBranch;
        this.description = description;
        this.githubLastUpdatedAt = githubLastUpdatedAt;
    }

    public void updateWithAnalysis(AnalysisResultDto result) {
        this.defaultBranch = result.getDefaultBranch();
        this.description = result.getDescription();
        this.githubLastUpdatedAt = result.getGithubLastUpdatedAt();
    }
}