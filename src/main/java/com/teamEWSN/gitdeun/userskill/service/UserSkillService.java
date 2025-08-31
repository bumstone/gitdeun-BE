package com.teamEWSN.gitdeun.userskill.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.userskill.dto.CategorizedSkillsWithSelectionDto;
import com.teamEWSN.gitdeun.userskill.dto.DeveloperSkillDto;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkillEnum;
import com.teamEWSN.gitdeun.userskill.entity.UserSkill;
import com.teamEWSN.gitdeun.userskill.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillService {

    private final UserRepository userRepository;
    private final UserSkillRepository userSkillRepository;

    // 기술 최대 선택 개수 상수화
    private static final int MAX_SKILL_COUNT = 15;

    /**
     * 사용자의 기술 목록과 선택 여부를 함께 조회
     *
     * @param userId 사용자 ID
     * @return 카테고리별로 분류된 기술 목록 (선택 여부 포함)
     */
    @Cacheable(value = "userSkills", key = "#userId")
    @Transactional(readOnly = true)
    public CategorizedSkillsWithSelectionDto getUserSkillsWithSelection(Long userId) {

        // 사용자가 선택한 기술 목록을 Set으로 변환 (검색 성능 향상)
        Set<String> selectedSkills = userSkillRepository.findByUserId(userId).stream()
            .map(UserSkill::getSkill)
            .collect(Collectors.toSet());

        // 카테고리별로 모든 기술을 분류하고 선택 여부 표시
        Map<String, List<DeveloperSkillDto>> categorizedSkills =
            Arrays.stream(DeveloperSkillEnum.values())
                .collect(Collectors.groupingBy(
                    DeveloperSkillEnum::getCategory,
                    LinkedHashMap::new,  // 순서 보장
                    Collectors.mapping(
                        skillEnum -> new DeveloperSkillDto(
                            skillEnum.getDisplayName(),
                            selectedSkills.contains(skillEnum.getDisplayName())
                        ),
                        Collectors.toList()
                    )
                ));

        log.debug("사용자 기술 조회 완료 - userId: {}, 선택된 기술 수: {}", userId, selectedSkills.size());

        return new CategorizedSkillsWithSelectionDto(categorizedSkills);
    }

    /**
     * 사용자가 선택한 기술 목록을 저장
     *
     * @param userId 사용자 ID
     * @param selectedSkills 선택한 기술 목록
     */
    @CacheEvict(value = "userSkills", key = "#userId")
    @Transactional
    public void saveUserSkills(Long userId, List<String> selectedSkills) {
        // null 체크 및 빈 리스트 처리
        if (selectedSkills == null) {
            selectedSkills = Collections.emptyList();
        }

        // 비즈니스 규칙 검증
        validateSkillSelection(selectedSkills);

        // 사용자 존재 여부 확인
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 기존 기술 목록 삭제
        List<UserSkill> existingSkills = userSkillRepository.findByUserId(userId);
        userSkillRepository.deleteAll(existingSkills);

        // 새로운 기술 목록 저장
        List<UserSkill> newSkills = selectedSkills.stream()
            .map(skill -> UserSkill.builder()
                .user(user)
                .skill(skill)
                .build())
            .collect(Collectors.toList());

        userSkillRepository.saveAll(newSkills);

        log.info("사용자 기술 갱신 완료 - userId: {}, 기술 수: {} -> {}",
            userId, existingSkills.size(), selectedSkills.size());
    }

    /**
     * 기술 선택 유효성 검증
     */
    private void validateSkillSelection(List<String> selectedSkills) {
        // 최대 선택 개수 검증
        if (selectedSkills.size() > MAX_SKILL_COUNT) {
            throw new GlobalException(ErrorCode.SKILL_LIMIT_EXCEEDED);
        }

        // 유효한 기술인지 검증 (Set으로 변환하여 검색 성능 향상)
        Set<String> validSkills = Arrays.stream(DeveloperSkillEnum.values())
            .map(DeveloperSkillEnum::getDisplayName)
            .collect(Collectors.toSet());

        // 유효하지 않은 기술 리스트 확인
        List<String> invalidSkills = selectedSkills.stream()
            .filter(skill -> !validSkills.contains(skill))
            .collect(Collectors.toList());

        if (!invalidSkills.isEmpty()) {
            log.warn("유효하지 않은 기술 선택 시도 - invalidSkills: {}", invalidSkills);
            throw new GlobalException(ErrorCode.INVALID_SKILL);
        }
    }
}
