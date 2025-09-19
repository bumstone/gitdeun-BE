package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.common.fastapi.dto.EdgeDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.NodeDto;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class MindmapGraphResponseDto {
    private Boolean success;
    private String error;

    // FastAPI 응답 데이터 그대로 전달
    private String graphMapId;
    private Integer nodeCount;
    private List<NodeDto> nodes;
    private List<EdgeDto> edges;
}