package com.teamEWSN.gitdeun.codereference.mapper;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CodeReferenceMapper {

    /**
     * 기본 CodeReference 응답 매핑
     */
    @Mapping(source = "id", target = "referenceId")
    ReferenceResponse toReferenceResponse(CodeReference codeReference);

    /**
     * 파일명 추출 헬퍼 메서드
     */
    @Named("extractFileName")
    default String extractFileName(String filePath) {
        if (filePath == null) return null;
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }
}