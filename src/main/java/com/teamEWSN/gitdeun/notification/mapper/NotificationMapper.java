package com.teamEWSN.gitdeun.notification.mapper;

import com.teamEWSN.gitdeun.notification.dto.NotificationResponseDto;
import com.teamEWSN.gitdeun.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface NotificationMapper {

    @Mapping(source = "id", target = "notificationId")
    NotificationResponseDto toResponseDto(Notification notification);
}
