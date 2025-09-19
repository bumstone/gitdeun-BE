package com.teamEWSN.gitdeun.common.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import java.util.List;

@Getter
public class MindmapGraphDto {
    @JsonProperty("map_id")
    private String mapId;
    private Integer count;
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;
}
