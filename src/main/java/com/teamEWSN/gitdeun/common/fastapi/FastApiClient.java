package com.teamEWSN.gitdeun.common.fastapi;

import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.ArangoDataDto;
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

    // FastAPI 서버에 리포지토리 분석을 요청
    public AnalysisResultDto analyze(String repoUrl, String prompt, MindmapType type, String authorizationHeader) {
        AnalysisRequest requestBody = new AnalysisRequest(repoUrl, prompt, type);
        // FastAPI 요청 본문을 위한 내부 DTO

        return webClient.post()
            .uri("/analyze") // FastAPI에 정의된 분석 엔드포인트
            .header("Authorization", authorizationHeader)
            .body(Mono.just(requestBody), AnalysisRequest.class)
            .retrieve() // 응답을 받아옴
            .bodyToMono(AnalysisResultDto.class) // 응답 본문을 DTO로 변환
            .block(); // 비동기 처리를 동기적으로 대기
    }

    // ArangoDB에서 마인드맵 데이터를 조회
    public ArangoDataDto getArangoData(String arangodbKey, String authorizationHeader) {
        return webClient.get()
            .uri("/arango/data/{key}", arangodbKey) // ArangoDB 데이터 조회 엔드포인트
            .header("Authorization", authorizationHeader)
            .retrieve()
            .bodyToMono(ArangoDataDto.class)
            .block();
    }

    // ArangoDB에 마인드맵 데이터를 저장하고 키를 반환
    public String saveArangoData(String repoUrl, String mapData, String authorizationHeader) {
        ArangoSaveRequest requestBody = new ArangoSaveRequest(repoUrl, mapData);

        return webClient.post()
            .uri("/arango/save") // ArangoDB 데이터 저장 엔드포인트
            .header("Authorization", authorizationHeader)
            .body(Mono.just(requestBody), ArangoSaveRequest.class)
            .retrieve()
            .bodyToMono(ArangoSaveResponse.class)
            .map(ArangoSaveResponse::getArangodbKey)
            .block();
    }

    // ArangoDB에서 마인드맵 데이터를 업데이트
    public ArangoDataDto updateArangoData(String arangodbKey, String mapData, String authorizationHeader) {
        ArangoUpdateRequest requestBody = new ArangoUpdateRequest(mapData);

        return webClient.put()
            .uri("/arango/data/{key}", arangodbKey) // ArangoDB 데이터 업데이트 엔드포인트
            .header("Authorization", authorizationHeader)
            .body(Mono.just(requestBody), ArangoUpdateRequest.class)
            .retrieve()
            .bodyToMono(ArangoDataDto.class)
            .block();
    }

    // ArangoDB에서 마인드맵 데이터를 삭제
    public void deleteAnalysisData(String arangodbKey) {
        webClient.delete()
            .uri("/arango/data/{key}", arangodbKey) // ArangoDB 데이터 삭제 엔드포인트
            .retrieve()
            .bodyToMono(Void.class)
            .block();
    }

    @Getter
    @AllArgsConstructor
    private static class AnalysisRequest {
        private String url;
        private String prompt;
        private MindmapType type;
    }

    @Getter
    @AllArgsConstructor
    private static class ArangoSaveRequest {
        private String repoUrl;
        private String mapData;
    }

    @Getter
    @AllArgsConstructor
    private static class ArangoUpdateRequest {
        private String mapData;
    }

    @Getter
    @AllArgsConstructor
    private static class ArangoSaveResponse {
        private String arangodbKey;
        private String status;
    }
}
