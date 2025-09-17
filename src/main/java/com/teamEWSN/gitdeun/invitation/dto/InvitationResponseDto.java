package com.teamEWSN.gitdeun.invitation.dto;

import com.teamEWSN.gitdeun.invitation.entity.InvitationStatus;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class InvitationResponseDto {

    private Long invitationId;
    private String mindmapTitle;
    private String inviteeName;
    private String inviteeEmail;
    private MindmapRole role;
    private InvitationStatus status;
    private LocalDateTime createdAt;
}