package com.teamEWSN.gitdeun.common.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
public class NodeDto {
    private String key;

    private String label;

    @JsonProperty("related_files")
    private List<String> relatedFiles;

    @JsonProperty("node_type")
    private String nodeType;

    private String mode;

    // 편의 메서드
    public boolean isFileNode() {
        return "file".equals(nodeType);
    }

    public boolean isSuggestionNode() {
        return "suggestion".equals(nodeType);
    }

    public int getFileCount() {
        return relatedFiles != null ? relatedFiles.size() : 0;
    }
}