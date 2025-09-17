package com.teamEWSN.gitdeun.common.fastapi;

import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class FastApiClient {

    private final WebClient webClient;

    public FastApiClient(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * GitHub 저장소의 최신 커밋 시간 조회
     */
    public LocalDateTime getRepositoryLastCommitTime(String repoUrl, String authorizationHeader) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/github/repos/last-commit")
                .queryParam("repo_url", repoUrl)
                .build())
            .header("Authorization", authorizationHeader)
            .retrieve()
            .bodyToMono(RepositoryCommitInfo.class)
            .map(RepositoryCommitInfo::getLastCommitTime)
            .block();
    }

    /**
     * FastAPI 서버에 리포지토리 프롬프트 기반 마인드맵 분석
     */
    public AnalysisResultDto analyzeWithPrompt(String repoUrl, String prompt, String authorizationHeader) {
        AnalysisRequest requestBody = new AnalysisRequest(repoUrl, prompt);
        return webClient.post()
            .uri("/mindmap/analyze-prompt")
            .header("Authorization", authorizationHeader)
            .body(Mono.just(requestBody), AnalysisRequest.class)
            .retrieve()
            .bodyToMono(AnalysisResultDto.class)
            .block();
    }

    /**
     * 기본 마인드맵 분석 (프롬프트 X)
     */
    public AnalysisResultDto analyzeDefault(String repoUrl, String authorizationHeader) {
        AnalysisRequest requestBody = new AnalysisRequest(repoUrl, null);
        return webClient.post()
            .uri("/mindmap/analyze-ai")
            .header("Authorization", authorizationHeader)
            .body(Mono.just(requestBody), AnalysisRequest.class)
            .retrieve()
            .bodyToMono(AnalysisResultDto.class)
            .block();
    }

    /**
     * ArangoDB에서 마인드맵 그래프 데이터를 조회
     */
    public MindmapGraphDto getMindmapGraph(String repoUrl, String authorizationHeader) {
        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/mindmap/graph")
                .queryParam("repo_url", repoUrl)
                .build())
            .header("Authorization", authorizationHeader)
            .retrieve()
            .bodyToMono(MindmapGraphDto.class)
            .block();
    }

    /**
     * GitHub 저장소 정보 저장을 요청
     */
    public void saveRepoInfo(String repoUrl, String authorizationHeader) {
        webClient.post()
            .uri("/repo/github/repo-info")
            .header("Authorization", authorizationHeader)
            .body(Mono.just(new GitRepoRequest(repoUrl)), GitRepoRequest.class)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    /**
     * GitHub ZIP을 ArangoDB에 저장을 요청
     */
    public void fetchRepo(String repoUrl, String authorizationHeader) {
        webClient.post()
            .uri("/github/repos/fetch")
            .header("Authorization", authorizationHeader)
            .body(Mono.just(new GitRepoRequest(repoUrl)), GitRepoRequest.class)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    /**
     * ArangoDB에서 repo_url 기반으로 마인드맵 데이터를 삭제
     */
    public void deleteMindmapData(String repoUrl, String authorizationHeader) {
        webClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/mindmap/repo")
                .queryParam("repo_url", repoUrl)
                .build())
            .header("Authorization", authorizationHeader)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // === Inner Classes ===

    @Getter
    @AllArgsConstructor
    private static class AnalysisRequest {
        private String repo_url;
        private String prompt;     // nullable
    }

    @Getter
    @AllArgsConstructor
    private static class GitRepoRequest {
        private String repo_url;
    }

    @Getter
    public static class RepositoryCommitInfo {
        private LocalDateTime lastCommitTime;
        private String lastCommitSha;
        private String defaultBranch;
    }
}