package com.teamEWSN.gitdeun.common.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class EdgeDto {
    @JsonProperty("from")
    private String fromKey;

    @JsonProperty("to")
    private String toKey;

    @JsonProperty("edge_type")
    private String edgeType;

    // 편의 메서드
    public boolean isContainmentEdge() {
        return "contains".equals(edgeType) || edgeType == null;
    }

    public boolean isSuggestionEdge() {
        return "suggestion".equals(edgeType);
    }
}
