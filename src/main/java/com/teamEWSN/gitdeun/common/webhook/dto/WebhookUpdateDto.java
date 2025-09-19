package com.teamEWSN.gitdeun.common.webhook.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class WebhookUpdateDto {
    // FastAPI 콜백 페이로드와 동일
    private String repoUrl;
    private String defaultBranch;
    private LocalDateTime lastCommit;
}
