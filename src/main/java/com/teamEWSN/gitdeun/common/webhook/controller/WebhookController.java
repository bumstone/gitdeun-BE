package com.teamEWSN.gitdeun.common.webhook.controller;

import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhook/mindmaps")
@RequiredArgsConstructor
public class WebhookController {

    private final MindmapService mindmapService;

    @PostMapping("/update")
    public ResponseEntity<Void> updateMindmapFromWebhook(@RequestHeader("Authorization") String authorizationHeader,
                                                         @RequestBody WebhookUpdateDto updateDto) {
        mindmapService.updateMindmapFromWebhook(updateDto, authorizationHeader);
        return ResponseEntity.ok().build();
    }
}