package com.teamEWSN.gitdeun.visithistory.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.visithistory.dto.VisitHistoryResponseDto;
import com.teamEWSN.gitdeun.visithistory.service.VisitHistoryBroadcastService;
import com.teamEWSN.gitdeun.visithistory.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class VisitHistoryController {

    private final VisitHistoryService visitHistoryService;
    private final VisitHistoryBroadcastService visitHistoryBroadcastService;


    /**
     * 방문 기록 실시간 연결 (SSE)
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamVisitHistoryUpdates(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        log.info("방문기록 SSE 연결 요청 - 사용자 ID: {}", userDetails.getId());
        return visitHistoryBroadcastService.createVisitHistoryConnection(userDetails.getId());
    }

    // 핀 고정되지 않은 방문 기록 조회
    @GetMapping("/visits")
    public ResponseEntity<Page<VisitHistoryResponseDto>> getVisitHistories(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10, sort = "lastVisitedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<VisitHistoryResponseDto> histories = visitHistoryService.getVisitHistories(userDetails.getId(), pageable);
        return ResponseEntity.ok(histories);
    }

    // 핀 고정된 방문 기록 조회
    @GetMapping("/pins")
    public ResponseEntity<List<VisitHistoryResponseDto>> getPinnedHistories(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<VisitHistoryResponseDto> histories = visitHistoryService.getPinnedHistories(userDetails.getId());
        return ResponseEntity.ok(histories);
    }

    // 방문 기록 삭제
    @DeleteMapping("/visits/{historyId}")
    public ResponseEntity<Void> deleteVisitHistory(
        @PathVariable Long historyId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        visitHistoryService.deleteVisitHistory(historyId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

}