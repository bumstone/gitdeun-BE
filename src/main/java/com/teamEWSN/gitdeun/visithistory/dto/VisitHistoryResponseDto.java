package com.teamEWSN.gitdeun.visithistory.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VisitHistoryResponseDto {
    private Long visitHistoryId;
    private Long mindmapId;
    private String mindmapField; // 마인드맵 제목
    private String repoUrl;
    private LocalDateTime lastVisitedAt;
}