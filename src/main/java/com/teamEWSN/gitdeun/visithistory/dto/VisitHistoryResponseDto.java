package com.teamEWSN.gitdeun.visithistory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VisitHistoryResponseDto {
    private Long visitHistoryId;
    private Long mindmapId;
    private String mindmapTitle;
    private String repoUrl;
    private LocalDateTime lastVisitedAt;
}