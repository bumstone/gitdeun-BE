package com.teamEWSN.gitdeun.mindmap.mapper;

import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromptHistoryMapper {

    /**
     * PromptHistory 엔티티 → PromptHistoryResponseDto 변환
     */
    @Mapping(source = "id", target = "historyId")
    PromptHistoryResponseDto toResponseDto(PromptHistory promptHistory);

    /**
     * PromptHistory 엔티티 → PromptPreviewResponseDto 변환
     */
    @Mapping(source = "id", target = "historyId")
    @Mapping(source = "mapData", target = "previewMapData")
    PromptPreviewResponseDto toPreviewResponseDto(PromptHistory promptHistory);

    /**
     * PromptHistory 리스트 → PromptHistoryResponseDto 리스트 변환
     */
    List<PromptHistoryResponseDto> toResponseDtoList(List<PromptHistory> promptHistories);

}