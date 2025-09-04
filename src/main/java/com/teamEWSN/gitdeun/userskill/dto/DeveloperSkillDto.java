package com.teamEWSN.gitdeun.userskill.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DeveloperSkillDto {
    private String name;      // 관심사 이름
    private boolean selected; // 선택 여부
}
