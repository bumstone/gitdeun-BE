package com.teamEWSN.gitdeun.invitation.dto;

import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InviteRequestDto {
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @NotNull(message = "이메일을 입력해주세요.")
    private String email;

    @NotNull(message = "권한을 선택해주세요.")
    private MindmapRole role;
}