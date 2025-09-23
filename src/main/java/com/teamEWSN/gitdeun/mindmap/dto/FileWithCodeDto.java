package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.common.fastapi.dto.RelatedFileDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileWithCodeDto {
    private String fileName;
    private String filePath;
    private String codeContents;

}
