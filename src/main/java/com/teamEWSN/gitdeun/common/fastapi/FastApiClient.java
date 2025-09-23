package com.teamEWSN.gitdeun.common.fastapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.teamEWSN.gitdeun.common.converter.IsoToLocalDateTimeDeserializer;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class FastApiClient {

    private final WebClient webClient;

    public FastApiClient(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }


    /**
     * 통합 분석 프로세스 - 3단계로 구성
     * 1. GitHub 저장소 fetch
     * 2. 기본 마인드맵 분석
     * 3. 그래프 데이터 조회 및 DTO 구성
     */
    public AnalysisResultDto analyzeResult(String repoUrl, String prompt, String authorizationHeader) {
        String mapId = extractMapId(repoUrl);
        log.info("저장소 분석 시작 - mapId: {}", mapId);

        try {
            // Step 1: GitHub 저장소를 ArangoDB에 저장
            FetchResponse fetchResult = fetchRepoInfo(repoUrl, authorizationHeader);
            log.info("Fetch 완료 - 파일: {}, 파싱: {}",
                fetchResult.getFiles_saved(), fetchResult.getFiles_parsed());

            saveRepoInfo(repoUrl, authorizationHeader);

            // Step 2: 마인드맵 기본 분석 (AI)
            AnalyzeResponse analyzeResult = analyzeAI(repoUrl, prompt, authorizationHeader);
            log.info("AI 분석 완료 - 디렉터리: {}", analyzeResult.getDirs_analyzed());

            // Step 3: 저장소 정보 조회
            RepoInfoResponse repoInfo = getRepoInfo(mapId, authorizationHeader);

            // 모든 데이터를 종합하여 DTO 생성
            return buildAnalysisResultDto(repoInfo);

        } catch (Exception e) {
            log.error("저장소 분석 실패: {}", e.getMessage(), e);
            return AnalysisResultDto.builder()
                .errorMessage("분석 실패: " + e.getMessage())
                .build();
        }
    }

    /**
     * 마인드맵 새로고침 - refresh-latest 엔드포인트 사용
     */
    public AnalysisResultDto refreshMindmap(String repoUrl, String prompt, String authorizationHeader) {
        String mapId = extractMapId(repoUrl);
        log.info("마인드맵 새로고침 시작 - mapId: {}, 프롬프트 사용: {}", mapId, StringUtils.hasText(prompt));

        try {
            // Step 1: 최신 변경사항만 빠르게 새로고침
            RefreshResponse refreshResult = refreshLatest(mapId, prompt, repoUrl, authorizationHeader);
            log.info("새로고침 완료 - 변경 파일: {}, 분석 디렉터리: {}",
                refreshResult.getChanged_files(), refreshResult.getDirs_analyzed());

            // Step 2: 저장소 정보 조회
            saveRepoInfo(repoUrl, authorizationHeader);
            RepoInfoResponse repoInfo = getRepoInfo(mapId, authorizationHeader);

            // 새로고침 결과를 DTO로 변환
            return buildRefreshResultDto(repoInfo);

        } catch (Exception e) {
            log.error("마인드맵 새로고침 실패: {}", e.getMessage(), e);
            return AnalysisResultDto.builder()
                .errorMessage("새로고침 실패: " + e.getMessage())
                .build();
        }
    }

    // 저장소 파일 fetch
    public FetchResponse fetchRepoInfo(String repoUrl, String authHeader) {
        Map<String, String> request = new HashMap<>();
        request.put("repo_url", repoUrl);

        return webClient.post()
            .uri("/github/repos/fetch")
            .header("Authorization", authHeader)
            .body(Mono.just(request), Map.class)
            .retrieve()
            .bodyToMono(FetchResponse.class)
            .block();
    }

    // 저장소 정보 저장
    public void saveRepoInfo(String repoUrl, String authHeader) {
        Map<String, String> request = new HashMap<>();
        request.put("repo_url", repoUrl);

        webClient.post()
            .uri("/repo/github/repo-info")
            .header("Authorization", authHeader)
            .body(Mono.just(request), Map.class)
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    // 저장소 정보 조회
    public RepoInfoResponse getRepoInfo(String mapId, String authHeader) {
        try {
            return webClient.get()
                .uri("/repo/{mapId}/info", mapId)
                .header("Authorization", authHeader)
                .retrieve()
                .bodyToMono(RepoInfoResponse.class)
                .block();
        } catch (Exception e) {
            log.warn("저장소 정보 조회 실패: {}, 기본값 사용", e.getMessage());
            // 조회 실패 시 기본값 반환
            RepoInfoResponse info = new RepoInfoResponse();
            info.setDefaultBranch("main");
            info.setLastCommit(LocalDateTime.now());
            return info;
        }
    }

    /**
     * 기본 마인드맵 분석
     */
    public AnalyzeResponse analyzeAI(String repoUrl, String prompt, String authHeader) {
        Map<String, String> request = new HashMap<>();
        request.put("repo_url", repoUrl);

        // 프롬프트가 있으면 추가, 없으면 기본 분석
        if (StringUtils.hasText(prompt)) {
            request.put("prompt", prompt);
        }

        return webClient.post()
            .uri("/mindmap/analyze-ai")
            .header("Authorization", authHeader)
            .body(Mono.just(request), Map.class)
            .retrieve()
            .bodyToMono(AnalyzeResponse.class)
            .block();
    }

    /**
     * ArangoDB에서 마인드맵 그래프 데이터를 조회
     */
    public MindmapGraphDto getGraph(String mapId, String authHeader) {
        return webClient.get()
            .uri("/mindmap/{mapId}/graph", mapId)
            .header("Authorization", authHeader)
            .retrieve()
            .bodyToMono(MindmapGraphDto.class)
            .block();
    }

    // TODO: 프롬프트 summary 반환

    // 최신 변경사항 새로고침
    public RefreshResponse refreshLatest(String mapId, String prompt, String repoUrl, String authHeader) {
        Map<String, Object> request = new HashMap<>();
        request.put("repo_url", repoUrl);
        if (StringUtils.hasText(prompt)) {
            request.put("prompt", prompt);
        }

        return webClient.post()
            .uri("/mindmap/{mapId}/refresh-latest", mapId)
            .header("Authorization", authHeader)
            .body(Mono.just(request), Map.class)
            .retrieve()
            .bodyToMono(RefreshResponse.class)
            .block();
    }

    /**
     * ArangoDB에서 repo_url 기반으로 마인드맵 데이터를 삭제
     */
    public void deleteMindmapData(String repoUrl, String authorizationHeader) {
        String mapId = extractMapId(repoUrl);

        webClient.delete()
            .uri(uriBuilder -> uriBuilder
                .path("/mindmap/{mapId}")
                .queryParam("also_recommendations", true)
                .build(mapId))
            .header("Authorization", authorizationHeader)
            .retrieve()
            .bodyToMono(DeleteResponse.class)
            .doOnSuccess(response -> log.info("삭제 완료 - 노드: {}, 엣지: {}",
                response.getNodes_removed(), response.getEdges_removed()))
            .block();
    }

    /**
     * 프롬프트 기반 자동 제안 생성 (auto endpoint)
     * - 프롬프트에서 스코프를 자동 추론
     * - 관련 파일들에 대한 코드 제안 일괄 생성
     */
    public SuggestionAutoResponse createAutoSuggestions(
        String repoUrl,
        String prompt,
        String authHeader) {

        String mapId = extractMapId(repoUrl);
        log.info("자동 제안 생성 시작 - mapId: {}, prompt: {}", mapId, prompt);

        Map<String, Object> request = new HashMap<>();
        request.put("repo_url", repoUrl);
        request.put("prompt", prompt);
        request.put("include_children", true);
        request.put("max_files", 12);
        request.put("return_code", true);

        try {
            return webClient.post()
                .uri("/suggestion/{mapId}/auto", mapId)
                .header("Authorization", authHeader)
                .body(Mono.just(request), Map.class)
                .retrieve()
                .bodyToMono(SuggestionAutoResponse.class)
                .block();
        } catch (Exception e) {
            log.error("자동 제안 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("제안 생성 실패: " + e.getMessage());
        }
    }

    public String getFileRaw(String repoUrl, String filePath, String authHeader) {
        return getFileRaw(repoUrl, filePath, null, null, null, authHeader);
    }

    public String getFileRaw(String repoUrl, String filePath, Integer startLine, Integer endLine, String sha, String authHeader) {
        String repoId = extractMapId(repoUrl);
        try {
            log.debug("FastAPI 파일 내용 조회 시작 - repoId: {}, filePath: {}", repoId, filePath);

            // URI 생성 (쿼리 파라미터 포함)
            UriComponentsBuilder uriBuilder = UriComponentsBuilder
                .fromPath("/content/file/raw")
                .queryParam("repo_id", repoId)
                .queryParam("path", filePath);

            if (startLine != null) {
                uriBuilder.queryParam("start_line", startLine);
            }
            if (endLine != null) {
                uriBuilder.queryParam("end_line", endLine);
            }
            if (sha != null && !sha.trim().isEmpty()) {
                uriBuilder.queryParam("sha", sha);
            }

            String uri = uriBuilder.build().toUriString();

            String response = webClient.get()
                .uri(uri)
                .headers(headers -> {
                    if (authHeader != null && !authHeader.trim().isEmpty()) {
                        headers.set("Authorization", authHeader);
                    }
                    headers.set("Accept", "text/plain");
                })
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
                    log.warn("FastAPI 파일 조회 4xx 오류 - repoId: {}, filePath: {}, status: {}",
                        repoId, filePath, clientResponse.statusCode());
                    return clientResponse.bodyToMono(String.class)
                        .map(errorBody -> new RuntimeException("파일을 찾을 수 없습니다: " + errorBody));
                })
                .onStatus(HttpStatusCode::is5xxServerError, serverResponse -> {
                    log.error("FastAPI 파일 조회 5xx 오류 - repoId: {}, filePath: {}, status: {}",
                        repoId, filePath, serverResponse.statusCode());
                    return Mono.error(new RuntimeException("FastAPI 서버 오류"));
                })
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();

            log.debug("FastAPI 파일 내용 조회 완료 - repoId: {}, filePath: {}, 길이: {}",
                repoId, filePath, response != null ? response.length() : 0);

            return response != null ? response : "";

        } catch (Exception e) {
            log.error("FastAPI 파일 내용 조회 실패 - repoId: {}, filePath: {}", repoId, filePath, e);
            return ""; // 빈 문자열 반환 (null 대신)
        }
    }

    // === Helper Methods ===

    // 저장소명 추출
    private String extractMapId(String repoUrl) {
        String[] segments = repoUrl.split("/");
        return segments[segments.length - 1].replaceAll("\\.git$", "");
    }

    private AnalysisResultDto buildAnalysisResultDto(
        RepoInfoResponse repoInfo
    ) {
        return AnalysisResultDto.builder()
            .defaultBranch(repoInfo.getDefaultBranch())
            .lastCommit(repoInfo.getLastCommit())
            .errorMessage(null)
            .build();
    }

    private AnalysisResultDto buildRefreshResultDto(
        RepoInfoResponse repoInfo
    ) {
        return AnalysisResultDto.builder()
            .defaultBranch(repoInfo.getDefaultBranch())
            .lastCommit(repoInfo.getLastCommit())
            .errorMessage(null)
            .build();
    }

    // === Response DTOs ===

    @Getter
    @Setter
    public static class FetchResponse {
        private String repo_id;
        private Integer files_saved;
        private Integer files_parsed;
    }

    @Getter
    @Setter
    public static class AnalyzeResponse {
        private String message;
        private String repo_id;
        private Integer dirs_analyzed;
    }

    @Getter
    @Setter
    public static class RefreshResponse {
        private String message;
        private String map_id;
        private String batch_time;
        private Integer changed_files;
        private Integer dirs_analyzed;
    }

    @Getter
    @Setter
    public static class RepoInfoResponse {
        @JsonProperty("default_branch")
        private String defaultBranch;

        @JsonProperty("last_commit")
        @JsonDeserialize(using = IsoToLocalDateTimeDeserializer.class)
        private LocalDateTime lastCommit;
    }

    @Getter
    @Setter
    public static class DeleteResponse {
        private String message;
        private String map_id;
        private Integer edges_removed;
        private Integer nodes_removed;
        private Integer recs_removed;
    }


    @Getter
    @Setter
    public static class SuggestionAutoResponse {
        private String map_id;
        private String prompt;
        private List<String> chosen_scopes;  // 선택된 스코프 (노드명)
        private List<String> candidates;
        private Integer total_target_files;
        private Integer created;
        private List<AutoScopeItem> items;
        private String aggregate_node_key;
    }

    @Getter
    @Setter
    public static class AutoScopeItem {
        private String scope_label;
        private String scope_node_key;
        private String file_path;
        private String suggestion_key;
        private String node_key;
        private String status;
        private String error;
        private String code;
    }

}