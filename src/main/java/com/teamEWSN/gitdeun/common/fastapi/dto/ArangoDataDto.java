package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ArangoDataDto {
    private String arangodbKey;
    private String mapData;
    private LocalDateTime updatedAt;
}
