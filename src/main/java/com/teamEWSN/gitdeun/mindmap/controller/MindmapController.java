package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequest;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import com.teamEWSN.gitdeun.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MindmapController {

    private final MindmapService mindmapService;


    @PostMapping
    public ResponseEntity<MindmapResponseDto> createMindmap(
        @RequestBody MindmapCreateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        MindmapResponseDto responseDto = mindmapService.createMindmap(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }


    // TODO: 마인드맵 방문 시 / 새로고침 시 업뎃 자동 확인 + 재동기화

}