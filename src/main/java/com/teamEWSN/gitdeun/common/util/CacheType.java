package com.teamEWSN.gitdeun.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    SERVICE_AUTOCOMPLETE("serviceAutocomplete", 2, 1200),   // 자동완성 캐시
    USER_SKILLS("userSkills", 1, 500),    // 사용자 개발기술 캐시
    MINDMAP_GRAPH_L1("mindmapGraphL1", 100, 1), // 30분, 최대 100개
    VISIT_HISTORY_SUMMARY("visitHistorySummary", 1000, 1), // 1시간, 최대 1000개
    FILE_CONTENT_L1("fileContentL1", 500, 1);

    private final String cacheName;
    private final int expiredAfterWrite;  // 시간(hour) 단위
    private final int maximumSize;        // 최대 캐시 항목 수
}