package com.teamEWSN.gitdeun.common.fastapi;

import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.FastApiCommitTimeResponse;
import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Component
public class FastApiClient {

    private final WebClient webClient; // FastApiConfig에서 생성한 Bean을 주입받음

    public FastApiClient(@Qualifier("fastApiWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * FastAPI 서버에 리포지토리 분석을 요청하고 그 결과를 받아옵니다.
     * @param repoUrl 분석할 리포지토리의 URL
     * @param prompt 분석에 사용할 프롬프트
     * @param type 분석 타입 (DEV, CHECK)
     * @return 분석 결과 DTO
     */
    public AnalysisResultDto analyze(String repoUrl, String prompt, MindmapType type) {
        // FastAPI 요청 본문을 위한 내부 DTO
        AnalysisRequest requestBody = new AnalysisRequest(repoUrl, prompt, type);

        return webClient.post()
            .uri("/analyze") // FastAPI에 정의된 분석 엔드포인트
            .body(Mono.just(requestBody), AnalysisRequest.class)
            .retrieve() // 응답을 받아옴
            .bodyToMono(AnalysisResultDto.class) // 응답 본문을 DTO로 변환
            .block(); // 비동기 처리를 동기적으로 대기
    }


    /**
     * FastAPI 서버에 특정 GitHub 리포지토리의 최신 커밋 시간을 요청합니다.
     * @param githubRepoUrl 조회할 리포지토리의 URL
     * @return 최신 커밋 시간
     */
    public LocalDateTime fetchLatestCommitTime(String githubRepoUrl) {
        // FastAPI의 가벼운 엔드포인트(예: /check-commit-time)를 호출합니다.
        FastApiCommitTimeResponse response = webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/check-commit-time") // FastAPI에 정의된 엔드포인트 경로
                .queryParam("url", githubRepoUrl) // 쿼리 파라미터로 URL 전달
                .build())
            .retrieve() // 응답을 받아옴
            .bodyToMono(FastApiCommitTimeResponse.class) // 응답 본문을 DTO로 변환
            .block(); // 비동기 응답을 동기적으로 기다림

        // null 체크 후 날짜 반환
        if (response == null) {
            throw new RuntimeException("FastAPI 서버로부터 최신 커밋 시간 정보를 받아오지 못했습니다.");
        }
        return response.getLatestCommitAt();
    }

    // TODO: requestAnalysis 등 다른 FastAPI 호출 메서드들도 여기에 구현



    @Getter
    @AllArgsConstructor
    private static class AnalysisRequest {
        private String url;
        private String prompt;
        private MindmapType type;
    }
}
