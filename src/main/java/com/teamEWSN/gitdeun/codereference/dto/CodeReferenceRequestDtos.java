package com.teamEWSN.gitdeun.codereference.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CodeReferenceRequestDtos {

    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotEmpty(message = "파일 경로는 비워둘 수 없습니다.")
        private String filePath;
        private Integer startLine;
        private Integer endLine;
    }

}
