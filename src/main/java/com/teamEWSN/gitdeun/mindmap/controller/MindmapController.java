package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("/api/mindmaps")
@RequiredArgsConstructor
public class MindmapController {

    private final MindmapService mindmapService;


    // 마인드맵 생성 (마인드맵에 한해서 owner 권한 얻음)
    @PostMapping
    public ResponseEntity<MindmapResponseDto> createMindmap(
        @RequestBody MindmapCreateRequestDto request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MindmapResponseDto responseDto = mindmapService.createMindmap(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // 마인드맵 상세 조회 (유저 인가 확인필요?)
    @GetMapping("/{mapId}")
    public ResponseEntity<MindmapDetailResponseDto> getMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MindmapDetailResponseDto responseDto = mindmapService.getMindmap(mapId, userDetails.getId());
        return ResponseEntity.ok(responseDto);
    }

    // 마인드맵 새로고침
    @PostMapping("/{mapId}/refresh")
    public ResponseEntity<MindmapDetailResponseDto> refreshMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MindmapDetailResponseDto responseDto = mindmapService.refreshMindmap(mapId, userDetails.getId());
        return ResponseEntity.ok(responseDto);
    }

    // 마인드맵 삭제 (owner만)
    @DeleteMapping("/{mapId}")
    public ResponseEntity<Void> deleteMindmap(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        mindmapService.deleteMindmap(mapId, userDetails.getId());
        return ResponseEntity.ok().build(); // 성공 시 200 OK와 빈 body 반환
    }


}