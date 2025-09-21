package com.teamEWSN.gitdeun.recruitment.service.util;

import com.teamEWSN.gitdeun.recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class RecommendationScoreCalculator {

    /**
     * 모집 공고와 사용자 기술 간의 매칭 점수를 계산
     * @param recruitment 모집 공고 엔티티
     * @param userSkills 사용자 기술 스택
     * @return 0.0 ~ 1.0 사이의 매칭 점수
     */
    public static double calculate(Recruitment recruitment, Set<DeveloperSkill> userSkills) {
        double score = 0.0;

        // 1. 기본 매칭 점수 계산 (단순 매칭만 사용)
        score = calculateSimpleScore(recruitment.getLanguageTags(), userSkills);

        // 2. 상태별 점수 조정
        score = applyStatusAdjustment(score, recruitment);

        // 3. 날짜별 점수 조정
        score = applyDateAdjustment(score, recruitment);

        return score;
    }

    /**
     * 단순 매칭 점수 계산 (모든 기술의 중요도가 동일)
     */
    private static double calculateSimpleScore(Set<DeveloperSkill> languageTags, Set<DeveloperSkill> userSkills) {
        if (languageTags.isEmpty()) return 0.0;

        long matchCount = languageTags.stream()
            .mapToLong(tag -> userSkills.contains(tag) ? 1 : 0)
            .sum();

        return (double) matchCount / languageTags.size();
    }

    /**
     * 공고 상태에 따른 점수 조정
     */
    private static double applyStatusAdjustment(double score, Recruitment recruitment) {
        // 모집 중인 공고가 모집 예정 공고보다 우선순위 높음
        if (recruitment.getStatus() == RecruitmentStatus.RECRUITING) {
            score += 0.05;
        }

        return Math.min(score, 1.0);
    }

    /**
     * 공고 등록일/마감일에 따른 점수 조정
     */
    private static double applyDateAdjustment(double score, Recruitment recruitment) {
        // 최근 등록된 공고에 가산점 부여
        if (recruitment.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            score += 0.05;
        }

        // 마감이 임박한 경우 가산점
        if (recruitment.getStatus() == RecruitmentStatus.RECRUITING) {
            long daysUntilEnd = ChronoUnit.DAYS.between(LocalDateTime.now(), recruitment.getEndAt());
            if (daysUntilEnd <= 2) {
                score += 0.03;
            }
        }

        // 모집 예정인 공고는 시작일이 가까울수록 점수 상승
        if (recruitment.getStatus() == RecruitmentStatus.FORTHCOMING) {
            long daysUntilStart = ChronoUnit.DAYS.between(LocalDateTime.now(), recruitment.getStartAt());

            if (daysUntilStart <= 3) {
                // 3일 이내에 시작하는 공고는 가산점
                score += 0.02;
            } else if (daysUntilStart > 14) {
                // 2주 이상 먼 공고는 감점
                score -= 0.05;
            }
        }

        return Math.min(Math.max(score, 0.0), 1.0); // 0~1 범위로 제한
    }
}
