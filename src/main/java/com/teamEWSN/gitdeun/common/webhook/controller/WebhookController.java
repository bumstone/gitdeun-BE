package com.teamEWSN.gitdeun.common.webhook.controller;

import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/mindmaps")
@RequiredArgsConstructor
public class WebhookController {

    private final MindmapService mindmapService;

    @PostMapping("/update")
    public ResponseEntity<Void> updateMindmapFromWebhook(@RequestBody WebhookUpdateDto updateDto) {
        mindmapService.updateMindmapFromWebhook(updateDto);
        return ResponseEntity.ok().build();
    }
}