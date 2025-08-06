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

//    @Column(name="file_name", length = 256, nullable = false)
//    private String fileName;

    @Column(length = 100)
    private String language;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;   // 기본 브랜치

    @Column(name = "github_last_updated_at")
    private LocalDateTime githubLastUpdatedAt; // GitHub 브랜치 최신 커밋 시간 (commit.committer.date)


    @Builder
    public Repo(String githubRepoUrl, String language, String defaultBranch, LocalDateTime githubLastUpdatedAt) {
        this.githubRepoUrl = githubRepoUrl;
        this.language = language;
        this.defaultBranch = defaultBranch;
        this.githubLastUpdatedAt = githubLastUpdatedAt;
    }

    public void updateWithAnalysis(AnalysisResultDto result) {
        this.language = result.getLanguage();
        this.defaultBranch = result.getDefaultBranch();
        this.githubLastUpdatedAt = result.getGithubLastUpdatedAt();
    }
}