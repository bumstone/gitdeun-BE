package com.teamEWSN.gitdeun.Recruitment.dto;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class RecruitmentDetailResponseDto {
    private Long id;
    private String title;
    private String content;
    private String contactEmail;
    private RecruitmentStatus status;

    private LocalDateTime startAt;
    private LocalDateTime endAt;

    private int teamSizeTotal;
    private int recruitQuota;
    private int viewCount;

    private String recruiterNickname;
    private String recruiterProfileImage;

    private Set<RecruitmentField> fieldTags;
    private Set<DeveloperSkill> languageTags;

    private List<RecruitmentImageDto> images;
}
