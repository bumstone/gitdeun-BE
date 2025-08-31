package com.teamEWSN.gitdeun.userskill.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class CategorizedSkillsWithSelectionDto {
    private Map<String, List<DeveloperSkillDto>> categorizedSkills;
}
