package com.teamEWSN.gitdeun.mindmap.controller;

import com.teamEWSN.gitdeun.mindmap.service.MindmapSseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
public class MindmapSseController {

    private final MindmapSseService mindmapSseService;

    // 클라이언트의 특정 마인드맵의 업데이트를 구독
    @GetMapping(value = "/api/mindmaps/{mapId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToMindmapUpdates(@PathVariable Long mapId) {
        return mindmapSseService.subscribe(mapId);
    }
}
