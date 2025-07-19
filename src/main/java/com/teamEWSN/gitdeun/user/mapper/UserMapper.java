package com.teamEWSN.gitdeun.user.mapper;

import com.teamEWSN.gitdeun.user.dto.UserResponseDto;
import com.teamEWSN.gitdeun.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {


    UserResponseDto toResponseDto(User user);

}
