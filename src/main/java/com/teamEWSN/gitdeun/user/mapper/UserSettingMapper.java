package com.teamEWSN.gitdeun.user.mapper;

import com.teamEWSN.gitdeun.user.dto.UserSettingResponseDto;
import com.teamEWSN.gitdeun.user.entity.UserSetting;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserSettingMapper {

    UserSettingResponseDto toResponseDto(UserSetting userSetting);

}