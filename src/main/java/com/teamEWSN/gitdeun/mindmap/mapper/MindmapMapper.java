package com.teamEWSN.gitdeun.mindmap.mapper;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MindmapMapper {

    /**
        * 마인드맵 기본 정보 매핑
     */
    @Mapping(source = "id", target = "mindmapId")
    @Mapping(source = "repo.id", target = "repoId")
    MindmapResponseDto toResponseDto(Mindmap mindmap);

    /**
     * 마인드맵 상세 정보 매핑 (프롬프트 히스토리 제외)
     */
    @Mapping(source = "id", target = "mindmapId")
    MindmapDetailResponseDto toDetailResponseDto(Mindmap mindmap);

    /**
     * 마인드맵 상세 정보 + 프롬프트 히스토리 매핑
     */
    @Mapping(source = "id", target = "mindmapId")
    @Mapping(source = "promptHistories", target = "promptHistories")
    @Mapping(source = "appliedPromptHistory", target = "appliedPromptHistory")
    MindmapDetailResponseDto toDetailResponseDtoWithHistory(
        Mindmap mindmap,
        List<PromptHistoryResponseDto> promptHistories,
        PromptHistoryResponseDto appliedPromptHistory
    );
}
