package com.teamEWSN.gitdeun.Recruitment.service;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.Recruitment.repository.RecruitmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruitmentSchedulingService {

    private final RecruitmentRepository recruitmentRepository;

    @Transactional
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정 실행
    public void updateRecruitmentStatus() {
        log.info("모집 공고 상태 업데이트 스케줄러 시작");
        LocalDateTime now = LocalDateTime.now();

        // 모집 예정 -> 모집 중
        List<Recruitment> startingRecruitments = recruitmentRepository
            .findAllByStatusAndStartAtBefore(RecruitmentStatus.FORTHCOMING, now);
        startingRecruitments.forEach(r -> r.setStatus(RecruitmentStatus.RECRUITING));
        log.info("{}개의 공고를 '모집 중'으로 변경했습니다.", startingRecruitments.size());

        // 모집 중 -> 모집 마감
        List<Recruitment> closingRecruitments = recruitmentRepository
            .findAllByStatusAndEndAtBefore(RecruitmentStatus.RECRUITING, now);
        closingRecruitments.forEach(r -> r.setStatus(RecruitmentStatus.CLOSED));
        log.info("{}개의 공고를 '모집 마감'으로 변경했습니다.", closingRecruitments.size());
        log.info("모집 공고 상태 업데이트 스케줄러 종료");
    }
}

