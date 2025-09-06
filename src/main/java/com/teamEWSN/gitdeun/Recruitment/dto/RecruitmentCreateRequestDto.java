package com.teamEWSN.gitdeun.Recruitment.dto;

import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import jakarta.validation.constraints.*;
import lombok.Getter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
public class RecruitmentCreateRequestDto {

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 120, message = "제목은 120자를 넘을 수 없습니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    @NotNull(message = "모집 시작일을 입력해주세요.")
    private LocalDateTime startAt;

    @NotNull(message = "모집 마감일을 입력해주세요.")
    @Future(message = "마감일은 현재보다 미래여야 합니다.")
    private LocalDateTime endAt;

    @NotNull(message = "총 팀원 수를 입력해주세요.")
    @Min(value = 1, message = "총 팀원 수는 1명 이상이어야 합니다.")
    private Integer teamSizeTotal;

    @NotNull(message = "모집 인원을 입력해주세요.")
    @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
    private Integer recruitQuota;

    @Size(min = 1, message = "모집 분야를 하나 이상 선택해주세요.")
    private Set<RecruitmentField> fieldTags;

    private Set<DeveloperSkill> languageTags;

    private List<MultipartFile> images;
}
