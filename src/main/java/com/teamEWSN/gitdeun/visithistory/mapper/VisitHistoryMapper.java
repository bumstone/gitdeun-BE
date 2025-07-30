package com.teamEWSN.gitdeun.visithistory.mapper;

import com.teamEWSN.gitdeun.visithistory.dto.VisitHistoryResponseDto;
import com.teamEWSN.gitdeun.visithistory.entity.VisitHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface VisitHistoryMapper {

    @Mapping(source = "id", target = "visitHistoryId")
    @Mapping(source = "mindmap.id", target = "mindmapId")
    @Mapping(source = "mindmap.field", target = "mindmapField")
    @Mapping(source = "mindmap.repo.githubRepoUrl", target = "repoUrl")
    VisitHistoryResponseDto toResponseDto(VisitHistory visitHistory);
}
