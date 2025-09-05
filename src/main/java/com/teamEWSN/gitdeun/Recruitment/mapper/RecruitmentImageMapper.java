package com.teamEWSN.gitdeun.Recruitment.mapper;

import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentImageDto;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecruitmentImageMapper {

    @Mapping(source = "id", target = "imageId")
    RecruitmentImageDto toDto(RecruitmentImage recruitmentImage);

    @Mapping(source = "imageId", target = "id")
    RecruitmentImage toEntity(RecruitmentImageDto dto);


}
