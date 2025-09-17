package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long>, RecruitmentCustomRepository{
    // 스케줄링을 위한 조회
    List<Recruitment> findAllByStatusAndStartAtBefore(RecruitmentStatus status, LocalDateTime now);
    List<Recruitment> findAllByStatusAndEndAtBefore(RecruitmentStatus status, LocalDateTime now);

    // 내 공고 목록 조회
    @EntityGraph(attributePaths = {"recruiter"})
    Page<Recruitment> findByRecruiterId(Long recruiterId, Pageable pageable);

    // 상태 기반 조회(추천 시)
    List<Recruitment> findAllByStatusIn(List<RecruitmentStatus> statuses);
}
