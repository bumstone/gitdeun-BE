package com.teamEWSN.gitdeun.invitation.mapper;

import com.teamEWSN.gitdeun.invitation.dto.InvitationResponseDto;
import com.teamEWSN.gitdeun.invitation.entity.Invitation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface InvitationMapper {

    @Mapping(source = "id", target = "invitationId")
    @Mapping(source = "mindmap.title", target = "mindmapTitle")
    @Mapping(source = "invitee.name", target = "inviteeName")
    @Mapping(source = "invitee.email", target = "inviteeEmail", defaultExpression = "java(\"링크 초대\")")
    InvitationResponseDto toResponseDto(Invitation invitation);
}
