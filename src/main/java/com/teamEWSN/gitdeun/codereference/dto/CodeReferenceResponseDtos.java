package com.teamEWSN.gitdeun.codereference.dto;

import lombok.*;

public class CodeReferenceResponseDtos {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReferenceResponse {
        private Long referenceId;
        private String nodeId;
        private String filePath;
        private Integer startLine;
        private Integer endLine;
    }

}