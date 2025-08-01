package com.teamEWSN.gitdeun.user.dto;

import com.teamEWSN.gitdeun.user.entity.UserSetting;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettingUpdateRequestDto {
    @NotNull(message = "테마를 선택해주세요.")
    private UserSetting.DisplayTheme theme;


    @NotNull(message = "이메일 수신 여부를 선택해주세요.")
    private Boolean emailNotification;
}
