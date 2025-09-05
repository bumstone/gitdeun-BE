package com.teamEWSN.gitdeun.Recruitment.mapper;

import com.teamEWSN.gitdeun.Recruitment.dto.*;
import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentImage;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import org.mapstruct.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RecruitmentMapper {

    @Mapping(source = "recruitment", target = "matchScore", qualifiedByName = "calculateScore")
    @Mapping(source = "recruitmentImages", target = "thumbnailUrl", qualifiedByName = "mapThumbnailUrl")
    RecruitmentListResponseDto toListResponseDto(Recruitment recruitment);

    @Mapping(source = "recruiter.nickname", target = "recruiterNickname")
    @Mapping(source = "recruiter.profileImage", target = "recruiterProfileImage")
    @Mapping(source = "recruitmentImages", target = "images", qualifiedByName = "mapImages")
    RecruitmentDetailResponseDto toDetailResponseDto(Recruitment recruitment);

    Recruitment toEntity(RecruitmentCreateRequestDto createDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateRecruitmentFromDto(RecruitmentUpdateRequestDto updateDto, @MappingTarget Recruitment recruitment);

    @Named("calculateScore")
    default Double calculateScore(Recruitment recruitment, @Context Set<DeveloperSkill> userSkills) {
        // 점수 계산 로직을 매퍼로 이동
        return RecommendationScoreCalculator.calculate(recruitment, userSkills);
    }

    /**
     * 썸네일 이미지 매핑 (첫 번째 이미지를 썸네일로 사용)
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
     * RecruitmentImage List를 RecruitmentImageDto List로 변환
     * (삭제되지 않은 이미지만 포함)
     */
    @Named("mapImages")
    default List<RecruitmentImageDto> mapImages(List<RecruitmentImage> recruitmentImages) {
        if (recruitmentImages == null || recruitmentImages.isEmpty()) {
            return null;
        }
        return recruitmentImages.stream()
            .filter(image -> image.getDeletedAt() == null) // soft delete 처리
            .map(this::toRecruitmentImageDto)
            .collect(Collectors.toList());
    }

    /**
     * RecruitmentImage를 RecruitmentImageDto로 변환
     */
    @Mapping(source = "id", target = "imageId")
    RecruitmentImageDto toRecruitmentImageDto(RecruitmentImage recruitmentImage);
}