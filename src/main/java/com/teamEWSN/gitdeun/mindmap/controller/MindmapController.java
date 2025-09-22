package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.*;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.MindmapPromptAnalysisDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptApplyRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.request.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapOrchestrationService;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import com.teamEWSN.gitdeun.mindmap.service.PromptHistoryService;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequestMapping("/api/mindmaps")
@RequiredArgsConstructor
public class MindmapController {

    private final MindmapService mindmapService;
    private final MindmapOrchestrationService mindmapOrchestrationService;
    private final MindmapAuthService mindmapAuthService;
    private final PromptHistoryService promptHistoryService;

    // 마인드맵 생성 (FastAPI 비동기 분석 기반)
    @PostMapping("/async")
    public ResponseEntity<MindmapCreationResponseDto> createMindmapAsync(
        @Valid @RequestBody MindmapCreateRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader) {

        Long userId = userDetails.getId();
        log.info("마인드맵 비동기 생성 요청 - 사용자: {}, 저장소: {}", userId, request.getRepoUrl());

        // 비동기 처리 호출
        mindmapOrchestrationService.createMindmap(request, userId, authorizationHeader);

        // 즉시 응답 반환
        MindmapCreationResponseDto response = MindmapCreationResponseDto.builder()
            .processId(String.format("mindmap_%d_%d", userId, System.currentTimeMillis()))
            .message("마인드맵 생성이 시작되었습니다. 완료되면 알림을 보내드립니다.")
            .build();

        return ResponseEntity.accepted().body(response);
    }


    // 마인드맵 상세 조회
    @GetMapping("/{mapId}")
    public ResponseEntity<MindmapDetailResponseDto> getMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        MindmapDetailResponseDto responseDto = mindmapService.getMindmap(mapId, userDetails.getId(), authorizationHeader);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 마인드맵 제목 수정
     */
    @PatchMapping("/{mapId}/title")
    public ResponseEntity<MindmapDetailResponseDto> updateMindmapTitle(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody MindmapTitleUpdateDto request
    ) {
        MindmapDetailResponseDto responseDto = mindmapService.updateMindmapTitle(
            mapId,
            userDetails.getId(),
            request);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 마인드맵 새로고침 (비동기)
     * - 요청을 즉시 반환하고 백그라운드에서 새로고침 진행
     */
    @PostMapping("/{mapId}/refresh")
    public ResponseEntity<Void> refreshMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        // 권한 검증은 동기적으로 먼저 수행
        if (!mindmapAuthService.hasView(mapId, userDetails.getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 비동기 서비스 호출
        mindmapOrchestrationService.refreshMindmap(mapId, userDetails.getId(), authorizationHeader);

        // 즉시 202 Accepted 응답 반환
        return ResponseEntity.accepted().build();
    }

    /**
     * 마인드맵 삭제 (동기 soft delete + 비동기 후처리)
     */
    @DeleteMapping("/{mapId}")
    public ResponseEntity<Void> deleteMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        // 1. DB soft delete - 즉시 처리
        Repo relatedRepo = mindmapService.deleteMindmap(mapId, userDetails.getId());

        // 2. ArangoDB 데이터 삭제 (비동기) - '실행 후 잊기'
        mindmapOrchestrationService.cleanUpMindmapData(relatedRepo.getGithubRepoUrl(), authorizationHeader);

        return ResponseEntity.ok().build();
    }

    // 프롬프트 기록 관련

    /**
     * 프롬프트 분석 및 미리보기 생성
     */
    @PostMapping("/{mapId}/prompts")
    public ResponseEntity<Void> analyzePrompt(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody MindmapPromptAnalysisDto request,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        // 권한 검증은 동기적으로 먼저 수행
        if (!mindmapAuthService.hasEdit(mapId, userDetails.getId())) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 비동기 서비스 호출
        mindmapOrchestrationService.promptMindmap(mapId, request.getPrompt(), userDetails.getId(), authorizationHeader);

        // 즉시 202 Accepted 응답 반환
        return ResponseEntity.accepted().build();
    }

    /**
     * 프롬프트 히스토리 적용
     */
    @PostMapping("/{mapId}/prompts/apply")
    public ResponseEntity<MindmapDetailResponseDto> applyPromptHistory(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody PromptApplyRequestDto request,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        promptHistoryService.applyPromptHistory(mapId, userDetails.getId(), request);

        MindmapDetailResponseDto responseDto = mindmapService.getMindmap(mapId, userDetails.getId(), authorizationHeader);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 프롬프트 히스토리 목록 조회 (페이징)
     */
    @GetMapping("/{mapId}/prompts/histories")
    public ResponseEntity<Page<PromptHistoryResponseDto>> getPromptHistories(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<PromptHistoryResponseDto> responseDto = promptHistoryService.getPromptHistories(mapId, userDetails.getId(), pageable);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 프롬프트 히스토리 미리보기 조회
     */
    @GetMapping("/{mapId}/prompts/histories/{historyId}/preview")
    public ResponseEntity<PromptPreviewResponseDto> getPromptHistoryPreview(
        @PathVariable Long mapId,
        @PathVariable Long historyId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PromptPreviewResponseDto responseDto = promptHistoryService.getPromptHistoryPreview(mapId, historyId, userDetails.getId());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 프롬프트 히스토리 삭제 (적용되지 않은 것만)
     */
    @DeleteMapping("/{mapId}/prompts/histories/{historyId}")
    public ResponseEntity<Void> deletePromptHistory(
        @PathVariable Long mapId,
        @PathVariable Long historyId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        promptHistoryService.deletePromptHistory(mapId, historyId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * 현재 적용된 프롬프트 히스토리 조회
     */
    @GetMapping("/{mapId}/prompts/applied")
    public ResponseEntity<PromptHistoryResponseDto> getAppliedPromptHistory(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(promptHistoryService.getAppliedPromptHistory(mapId, userDetails.getId()));
    }
}