package com.teamEWSN.gitdeun.recruitment.dto;

import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import jakarta.validation.constraints.*;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
public class RecruitmentUpdateRequestDto {
    @Size(max = 120, message = "제목은 120자를 넘을 수 없습니다.")
    private String title;

    private String content;

    @Email(message = "유효한 이메일 형식이 아닙니다.")
    private String contactEmail;

    @Future(message = "마감일은 현재보다 미래여야 합니다.")
    private LocalDateTime endAt;

    @Min(value = 1, message = "총 팀원 수는 1명 이상이어야 합니다.")
    private Integer teamSizeTotal;

    @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
    private Integer recruitQuota;

    @Size(min = 1, message = "모집 분야를 하나 이상 선택해주세요.")
    private Set<RecruitmentField> fieldTags;

    private Set<DeveloperSkill> languageTags;

    private List<Long> keepImageIds;                 // 유지할 기존 이미지 ID
}

