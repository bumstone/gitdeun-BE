package com.teamEWSN.gitdeun.user.dto;

import com.teamEWSN.gitdeun.user.entity.UserSetting;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSettingResponseDto {
    private UserSetting.DisplayTheme theme;
    private UserSetting.UserMode mode;
    private boolean emailNotification;
}
