package com.teamEWSN.gitdeun.common.fastapi.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter // JSON 역직렬화를 위해 필요
public class FastApiCommitTimeResponse {
    private LocalDateTime latestCommitAt;
}
