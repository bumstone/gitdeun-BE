package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.repo.entity.Repo;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MindmapCreationResultDto {
    private final Repo repo;
    private final String mapData;
    private final String title;
}
