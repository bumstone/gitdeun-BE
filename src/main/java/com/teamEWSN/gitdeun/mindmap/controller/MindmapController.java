package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.*;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.MindmapPromptAnalysisDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptApplyRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import com.teamEWSN.gitdeun.mindmap.service.PromptHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mindmaps")
@RequiredArgsConstructor
public class MindmapController {

    private final MindmapService mindmapService;
    private final PromptHistoryService promptHistoryService;

    // 마인드맵 생성 (FastAPI 분석 기반)
    @PostMapping
    public ResponseEntity<MindmapResponseDto> createMindmap(
        @RequestHeader("Authorization") String authorizationHeader,
        @RequestBody MindmapCreateRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MindmapResponseDto responseDto = mindmapService.createMindmap(
            request,
            userDetails.getId(),
            authorizationHeader
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
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
        MindmapDetailResponseDto responseDto = mindmapService.updateMindmapTitle(mapId, userDetails.getId(), request);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 마인드맵 새로고침
     */
    @PostMapping("/{mapId}/refresh")
    public ResponseEntity<MindmapDetailResponseDto> refreshMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        MindmapDetailResponseDto responseDto = mindmapService.refreshMindmap(mapId, userDetails.getId(), authorizationHeader);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 마인드맵 삭제 (owner만)
     */
    @DeleteMapping("/{mapId}")
    public ResponseEntity<Void> deleteMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        mindmapService.deleteMindmap(mapId, userDetails.getId(), authorizationHeader);
        return ResponseEntity.ok().build();
    }


    /**
     * 프롬프트 분석 및 미리보기 생성
     */
    @PostMapping("/{mapId}/prompts")
    public ResponseEntity<PromptPreviewResponseDto> analyzePromptPreview(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody MindmapPromptAnalysisDto request,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        PromptPreviewResponseDto responseDto = promptHistoryService.createPromptPreview(
            mapId,
            userDetails.getId(),
            request,
            authorizationHeader
        );
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 프롬프트 히스토리 적용
     */
    @PostMapping("/{mapId}/prompts/apply")
    public ResponseEntity<MindmapDetailResponseDto> applyPromptHistory(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody PromptApplyRequestDto request
    ) {
        promptHistoryService.applyPromptHistory(mapId, userDetails.getId(), request);

        // 적용 후 최신 마인드맵 정보 반환
        MindmapDetailResponseDto responseDto = mindmapService.getMindmap(mapId, userDetails.getId(), "");
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 프롬프트 히스토리 목록 조회
     */
    @GetMapping("/{mapId}/prompts/histories")
    public ResponseEntity<List<PromptHistoryResponseDto>> getPromptHistories(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<PromptHistoryResponseDto> responseDto = promptHistoryService.getPromptHistories(mapId, userDetails.getId());
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

}