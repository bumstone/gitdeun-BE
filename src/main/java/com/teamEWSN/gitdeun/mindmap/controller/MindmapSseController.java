package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.service.MindmapSseService;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mindmaps")
@RequiredArgsConstructor
public class MindmapSseController {

    private final MindmapSseService mindmapSseService;
    private final MindmapAuthService mindmapAuthService;

    /**
     * 마인드맵 실시간 연결 (SSE)
     */
    @GetMapping(value = "/{mapId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMindmapUpdates(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // VIEW 권한 확인
        if (!mindmapAuthService.hasView(mapId, userDetails.getId())) {
            log.warn("SSE 연결 권한 없음 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId());
            SseEmitter emitter = new SseEmitter(0L);
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("접근 권한이 없습니다."));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        log.info("SSE 연결 요청 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId());
        return mindmapSseService.createConnection(mapId, userDetails);
    }

    /**
     * 현재 연결된 사용자 수 조회
     */
    @GetMapping("/{mapId}/connections/count")
    public int getConnectionCount(@PathVariable Long mapId) {
        return mindmapSseService.getConnectionCount(mapId);
    }

    /**
     * 현재 연결된 사용자 목록 조회
     */
    @GetMapping("/{mapId}/connections/users")
    public ResponseEntity<List<MindmapSseService.ConnectedUserDto>> getConnectedUsers(@PathVariable Long mapId) {
        return ResponseEntity.ok(mindmapSseService.getConnectedUsers(mapId));
    }
}
