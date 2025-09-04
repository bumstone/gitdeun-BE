package com.teamEWSN.gitdeun.userskill.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class SkillSelectionRequestDto {
    private Map<String, List<String>> categorizedSkills;

    public List<String> getAllSkills() {
        if (categorizedSkills == null) {
            return new ArrayList<>();
        }
        List<String> allSkills = new ArrayList<>();
        categorizedSkills.values().forEach(allSkills::addAll);
        return allSkills;
    }
}