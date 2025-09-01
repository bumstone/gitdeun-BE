package com.teamEWSN.gitdeun.common.fastapi.controller;

import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
public class FastApiController {

    private final FastApiClient fastApiClient;
    private final MindmapService mindmapService;

    /**
     * 클라이언트의 마인드맵 생성 요청을 받아 FastAPI로 전달하고,
     * 그 결과를 사용하여 마인드맵을 생성하는 프록시 엔드포인트
     */
    @PostMapping("/mindmaps")
    public ResponseEntity<MindmapResponseDto> createMindmapViaProxy(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody MindmapCreateRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 1. Spring Boot가 FastAPI 서버에 분석을 요청
        AnalysisResultDto analysisResult = fastApiClient.analyze(
            request.getRepoUrl(),
            request.getPrompt(),
            request.getType(),
            authorizationHeader
        );

        // 2. FastAPI로부터 받은 분석 결과를 MindmapService로 전달하여 마인드맵 생성
        MindmapResponseDto responseDto = mindmapService.createMindmapFromAnalysis(
            request,
            analysisResult,
            userDetails.getId(),
            authorizationHeader
        );

        return ResponseEntity.ok(responseDto);
    }
}
