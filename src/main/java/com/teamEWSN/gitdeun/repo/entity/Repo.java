package com.teamEWSN.gitdeun.repo.entity;

import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;   // 기본 브랜치

    @Column(name = "last_commit")
    private LocalDateTime lastCommit; // GitHub 브랜치 최신 커밋 시간 (commit.committer.date)

    @OneToMany(mappedBy = "repo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Mindmap> mindmaps = new ArrayList<>();

    @Builder
    public Repo(String githubRepoUrl, String defaultBranch, LocalDateTime githubLastUpdatedAt) {
        this.githubRepoUrl = githubRepoUrl;
        this.defaultBranch = defaultBranch;
        this.lastCommit = githubLastUpdatedAt;
    }

    public void updateWithAnalysis(AnalysisResultDto result) {
        this.defaultBranch = result.getDefaultBranch();
        this.lastCommit = result.getLastCommit();
    }

    public void updateWithWebhookData(WebhookUpdateDto dto) {
        this.defaultBranch = dto.getDefaultBranch();
        this.lastCommit = dto.getLastCommit();
    }

    // 마지막 커밋 시간 업데이트
    public void updateLastCommitTime(LocalDateTime lastCommitTime) {
        if (lastCommitTime != null) {
            this.lastCommit = lastCommitTime;
        }
    }

    // 기본 브랜치 업데이트
    public void updateDefaultBranch(String defaultBranch) {
        if (StringUtils.hasText(defaultBranch)) {
            this.defaultBranch = defaultBranch;
        }
    }


    // 저장소가 최신 상태인지 확인
    public boolean isNewerThan(LocalDateTime comparisonTime) {
        if (this.lastCommit == null || comparisonTime == null) {
            return false;
        }
        return this.lastCommit.isAfter(comparisonTime);
    }
}