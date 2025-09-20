package com.teamEWSN.gitdeun.Application.dto;

import com.teamEWSN.gitdeun.Application.entity.ApplicationStatus;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentField;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationListResponseDto {

    private Long applicationId;

    // 지원자 간략 정보
    private Long applicantId;
    private String applicantName;
    private String applicantNickname;
    private String applicantProfileImage;

    // 공고 간략 정보
    private String recruitmentTitle;

    // 지원 정보
    private RecruitmentField appliedField;
    private ApplicationStatus status;
    private LocalDateTime createdAt;
}
