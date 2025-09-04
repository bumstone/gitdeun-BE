package com.teamEWSN.gitdeun.common.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CacheType {
    SERVICE_AUTOCOMPLETE("serviceAutocomplete", 2, 1200),   // 자동완성 캐시
    USER_SKILLS("userSkills", 1, 500);     // 사용자 개발기술 캐시

    private final String cacheName;
    private final int expiredAfterWrite;  // 시간(hour) 단위
    private final int maximumSize;        // 최대 캐시 항목 수
}