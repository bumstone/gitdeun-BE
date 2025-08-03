package com.teamEWSN.gitdeun.mindmapmember.dto;

import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RoleChangeRequestDto {
    private MindmapRole role;   // OWNER, EDITOR, VIEWER
}