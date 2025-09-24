package com.teamEWSN.gitdeun.mindmap.dto;

import com.teamEWSN.gitdeun.common.fastapi.dto.RelatedFileDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class FileWithCodeDto {
    private final String fileName;
    private final String filePath;
    private final String codeContents;
    private final int lineCount;

    public FileWithCodeDto(RelatedFileDto relatedFile, String codeContents) {
        this.fileName = relatedFile.getFileName();
        this.filePath = relatedFile.getFilePath();
        this.codeContents = codeContents;
        this.lineCount = codeContents == null ? 0 : codeContents.split("\\r?\\n").length;
    }
}
