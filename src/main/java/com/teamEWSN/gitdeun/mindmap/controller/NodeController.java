package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.NodeCodeResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.NodeSimpleDto;
import com.teamEWSN.gitdeun.mindmap.service.NodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mindmaps/{mapId}/nodes")
@RequiredArgsConstructor
public class NodeController {

    private final NodeService nodeService;

    /**
     * 특정 노드의 상세 정보와 연결된 모든 파일의 전체 코드를 조회합니다.
     * CodeReview 및 CodeReference 기능에 사용됩니다.
     */
    @GetMapping("/{nodeKey}/code")
    public ResponseEntity<NodeCodeResponseDto> getNodeWithCode(
        @PathVariable Long mapId,
        @PathVariable String nodeKey,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        // (인증/인가 로직은 Spring Security와 MindmapAuthService에서 처리)
        NodeCodeResponseDto response = nodeService.getNodeDetailsWithCode(mapId, nodeKey, userDetails.getId(), authorizationHeader);
        return ResponseEntity.ok(response);
    }

    /**
     * (테스트용) 마인드맵의 모든 노드 목록(key, label)을 조회합니다.
     */
    @GetMapping("/list")
    public ResponseEntity<List<NodeSimpleDto>> getNodeList(
        @PathVariable Long mapId,
        @RequestHeader("Authorization") String authorizationHeader
    ) {
        List<NodeSimpleDto> response = nodeService.getNodeList(mapId, authorizationHeader);
        return ResponseEntity.ok(response);
    }
}


