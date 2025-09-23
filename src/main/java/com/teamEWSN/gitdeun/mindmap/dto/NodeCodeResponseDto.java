package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.common.fastapi.dto.RelatedFileDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class NodeCodeResponseDto {
    private String nodeKey;
    private String nodeLabel;
    private List<RelatedFileDto> filePaths;
    private Map<RelatedFileDto, String> codeContents; // filePath -> full code content
}