package com.teamEWSN.gitdeun.Application.dto;

import com.teamEWSN.gitdeun.Application.entity.ApplicationStatus;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponseDto {

    private Long applicationId;

    // 지원자 정보
    private Long applicantId;
    private String applicantName;
    private String applicantEmail;
    private String applicantNickname;
    private String applicantProfileImage;

    // 공고 정보
    private Long recruitmentId;
    private String recruitmentTitle;
    private String recruiterName;

    // 지원 정보
    private RecruitmentField appliedField;
    private String message;
    private ApplicationStatus status;
    private String rejectReason;
    private boolean active;

    // 날짜 정보
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
