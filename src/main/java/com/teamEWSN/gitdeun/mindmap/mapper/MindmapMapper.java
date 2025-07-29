package com.teamEWSN.gitdeun.mindmap.mapper;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MindmapMapper {

    @Mapping(source = "id",  target = "mindmapId")
    MindmapResponseDto toResponseDto(Mindmap mindmap);

    @Mapping(source = "id",  target = "mindmapId")
    MindmapDetailResponseDto toDetailResponseDto(Mindmap mindmap);
}
