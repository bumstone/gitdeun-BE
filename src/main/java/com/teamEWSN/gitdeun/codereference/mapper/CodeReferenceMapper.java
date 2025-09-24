package com.teamEWSN.gitdeun.codereference.mapper;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CodeReferenceMapper {

    /**
     * 기본 CodeReference 응답 매핑
     */
    @Mapping(target = "referenceId", source = "id")
    @Mapping(source = "codeReviews", target = "reviewIds", qualifiedByName = "reviewsToIds")
    ReferenceResponse toReferenceResponse(CodeReference codeReference);

    @Mapping(target = "referenceId", source = "codeReference.id")
    @Mapping(target = "nodeKey", source = "codeReference.nodeKey")
    @Mapping(target = "filePath", source = "codeReference.filePath")
    @Mapping(target = "startLine", source = "codeReference.startLine")
    @Mapping(target = "endLine", source = "codeReference.endLine")
    @Mapping(target = "codeContent", expression = "java(codeContent)")
    ReferenceDetailResponse toReferenceDetailResponse(CodeReference codeReference, String codeContent);

    /**
     * 파일명 추출 헬퍼 메서드
     */
    @Named("extractFileName")
    default String extractFileName(String filePath) {
        if (filePath == null) return null;
        return filePath.substring(filePath.lastIndexOf('/') + 1);
    }

    @Named("reviewsToIds")
    default List<Long> reviewsToIds(List<CodeReview> reviews) {
        if (reviews == null) {
            return null;
        }
        return reviews.stream().map(CodeReview::getId).collect(Collectors.toList());
    }
}