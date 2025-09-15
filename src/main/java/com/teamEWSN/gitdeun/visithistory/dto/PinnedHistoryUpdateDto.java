package com.teamEWSN.gitdeun.visithistory.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class PinnedHistoryUpdateDto {

    /**
     * 액션 타입: PIN_ADDED, PIN_REMOVED, PIN_LIMIT_WARNING
     */
    private String action;

    private Long historyId;
    private Long mindmapId;

    private String mindmapTitle;

    private long currentPinCount;
    private int maxPinCount;

    // 타임스탬프 - 클라이언트 동기화
    @Builder.Default
    private long timestamp = System.currentTimeMillis();
}