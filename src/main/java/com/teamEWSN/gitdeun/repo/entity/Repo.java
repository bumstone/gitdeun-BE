package com.teamEWSN.gitdeun.repo.entity;

import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Repo")
public class Repo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_repo_url", length = 512, nullable = false)
    private String githubRepoUrl;

    @Column(name = "default_branch", length = 100)
    private String defaultBranch;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}