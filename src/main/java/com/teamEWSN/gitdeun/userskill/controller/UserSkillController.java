package com.teamEWSN.gitdeun.userskill.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.userskill.dto.CategorizedSkillsResponseDto;
import com.teamEWSN.gitdeun.userskill.dto.CategorizedSkillsWithSelectionDto;
import com.teamEWSN.gitdeun.userskill.dto.SkillSelectionRequestDto;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkillEnum;
import com.teamEWSN.gitdeun.userskill.service.UserSkillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class UserSkillController {

    UserSkillService userSkillService;

    // 선택할 개발 기술 목록 조회
    @GetMapping
    public ResponseEntity<CategorizedSkillsResponseDto> getAvailableInterests() {
        // Enum에서 카테고리별 displayName 값 추출
        Map<String, List<String>> categorizedInterests = Arrays.stream(DeveloperSkillEnum.values())
            .collect(Collectors.groupingBy(
                DeveloperSkillEnum::getCategory,
                Collectors.mapping(DeveloperSkillEnum::getDisplayName, Collectors.toList())
            ));
        return ResponseEntity.ok(new CategorizedSkillsResponseDto(categorizedInterests));
    }

    // 선택한 개발 기술 목록 조회
    @GetMapping("/me")
    public ResponseEntity<CategorizedSkillsWithSelectionDto> getMySkillsWithSelection(
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getId();
        return ResponseEntity.ok(userSkillService.getUserSkillsWithSelection(userId));
    }

    // 선택한 기술 저장
    @PostMapping("/me")
    public ResponseEntity<Void> saveSkills(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody SkillSelectionRequestDto requestDto
    ) {
        Long userId = userDetails.getId();
        userSkillService.saveUserSkills(userId, requestDto.getAllSkills());
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
