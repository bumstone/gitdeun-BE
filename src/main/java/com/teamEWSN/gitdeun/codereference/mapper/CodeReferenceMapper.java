package com.teamEWSN.gitdeun.codereference.mapper;

import com.teamEWSN.gitdeun.codereference.dto.CodeReferenceResponseDtos.*;
import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CodeReferenceMapper {

    @Mapping(target = "referenceId", source = "id")
    ReferenceResponse toReferenceResponse(CodeReference codeReference);
}