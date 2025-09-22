package com.teamEWSN.gitdeun.common.fastapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatedFileDto {

    /**
     * 파일 경로 (CodeReference.filePath와 매핑)
     * 예: "src/main/java/com/example/service/UserService.java"
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * 파일명만 추출
     * @return 파일명 (확장자 포함)
     */
    public String getFileName() {
        if (filePath == null) return null;
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}