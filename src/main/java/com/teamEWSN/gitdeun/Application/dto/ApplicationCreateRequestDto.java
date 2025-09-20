package com.teamEWSN.gitdeun.Application.dto;

import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentField;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationCreateRequestDto {

    @NotNull(message = "지원 분야를 선택해주세요.")
    private RecruitmentField appliedField;

    @Size(max = 1000, message = "지원 메시지는 1000자 이내로 작성해주세요.")
    private String message;
}