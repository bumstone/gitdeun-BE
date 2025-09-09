package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Getter;
import java.util.List;

@Getter
public class NodeDto {
    private String key;
    private String label;
    private List<String> related_files;
}