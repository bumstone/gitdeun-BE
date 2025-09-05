package com.teamEWSN.gitdeun.Recruitment.dto;

import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class RecruitmentListResponseDto {
    private Long id;
    private String title;
    private String thumbnailUrl;   // 썸네일 이미지(이미지 리스트 중 맨 앞)
    private RecruitmentStatus status;

    private Set<DeveloperSkill> languageTags;   // 개발 기술 및 지원 분야 태그
    private Set<RecruitmentField> fieldTags;

    private LocalDateTime startAt;  // 모집 기간
    private LocalDateTime endAt;

    private Integer views;
    private Integer recruitQuota;

    // private String recruiterNickname;

    private Double matchScore;
}
