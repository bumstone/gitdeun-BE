package com.teamEWSN.gitdeun.Recruitment.mapper;

import com.teamEWSN.gitdeun.Recruitment.dto.*;
import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentImage;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecruitmentMapper {

    @Mapping(source = "recruitmentImages", target = "thumbnailUrl", qualifiedByName = "mapThumbnailUrl")
    RecruitmentListResponseDto toListResponseDto(Recruitment recruitment);

    @Mapping(source = "recruiter.nickname", target = "recruiterNickname")
    @Mapping(source = "recruiter.profileImage", target = "recruiterProfileImage")
    @Mapping(source = "recruitmentImages", target = "images", qualifiedByName = "mapImages")
    RecruitmentDetailResponseDto toDetailResponseDto(Recruitment recruitment);

    Recruitment toEntity(RecruitmentCreateRequestDto createDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRecruitmentFromDto(RecruitmentUpdateRequestDto updateDto, @MappingTarget Recruitment recruitment);


    /**
     * 썸네일 URL 매핑 (이미지 목록의 첫 번째 이미지)
     */
    @Named("mapThumbnailUrl")
    default String mapThumbnailUrl(List<RecruitmentImage> recruitmentImages) {
        if (recruitmentImages == null || recruitmentImages.isEmpty()) {
            return null;
        }

        return recruitmentImages.stream()
            .filter(image -> image.getDeletedAt() == null)
            .findFirst()
            .map(RecruitmentImage::getImageUrl)
            .orElse(null);
    }

    /**
     * 이미지 리스트 매핑
     */
    @Named("mapImages")
    default List<RecruitmentImageDto> mapImages(List<RecruitmentImage> recruitmentImages) {
        if (recruitmentImages == null || recruitmentImages.isEmpty()) {
            return null;
        }
        return recruitmentImages.stream()
            .filter(image -> image.getDeletedAt() == null)
            .map(this::toRecruitmentImageDto)
            .collect(Collectors.toList());
    }

    /**
     * 이미지 DTO 변환
     */
    @Mapping(source = "id", target = "imageId")
    RecruitmentImageDto toRecruitmentImageDto(RecruitmentImage recruitmentImage);
}