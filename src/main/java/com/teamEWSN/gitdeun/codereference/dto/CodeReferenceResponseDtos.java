package com.teamEWSN.gitdeun.codereference.dto;

import lombok.*;

import java.util.List;

public class CodeReferenceResponseDtos {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReferenceResponse {
        private Long referenceId;
        private String nodeKey;
        private String filePath;
        private Integer startLine;
        private Integer endLine;
        private List<Long> reviewIds;
    }

    @Getter
    @Builder
    public static class ReferenceDetailResponse {
        private Long referenceId;
        private String nodeKey;
        private String filePath;
        private Integer startLine;
        private Integer endLine;
        private String codeContent; // 실제 코드 내용을 담을 필드
    }
}