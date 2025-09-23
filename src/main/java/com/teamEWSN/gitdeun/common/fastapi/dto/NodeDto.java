package com.teamEWSN.gitdeun.common.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class NodeDto {
    private String key;

    private String label;

    @JsonProperty("related_files")
    private List<RelatedFileDto> relatedFiles;

    @JsonProperty("node_type")
    private String nodeType;
}