package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class MindmapGraphDto {
    private int count;
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;
}
