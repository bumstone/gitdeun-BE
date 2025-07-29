package com.teamEWSN.gitdeun.mindmap.mapper;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MindmapMapper {

    MindmapResponseDto toResponseDto(Mindmap mindmap);
}
