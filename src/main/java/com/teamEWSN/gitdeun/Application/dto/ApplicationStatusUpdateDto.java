package com.teamEWSN.gitdeun.Application.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusUpdateDto {

    @Size(max = 500, message = "거절 사유는 500자 이내로 작성해주세요.")
    private String rejectReason;
}