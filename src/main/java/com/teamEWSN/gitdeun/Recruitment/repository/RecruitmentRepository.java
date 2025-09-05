package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecruitmentRepository extends JpaRepository<Recruitment, Long>, RecruitmentCustomRepository{
    // 스케줄링을 위한 조회
    List<Recruitment> findAllByStatusAndStartAtBefore(RecruitmentStatus status, LocalDateTime now);
    List<Recruitment> findAllByStatusAndEndAtBefore(RecruitmentStatus status, LocalDateTime now);

    // 내 공고 목록 조회
    Page<Recruitment> findByRecruiterId(Long recruiterId, Pageable pageable);

    // 공고 추천을 위한
    @Query("SELECT r FROM Recruitment r LEFT JOIN FETCH r.requiredSkills rs " + "WHERE r.status = :status")
    List<Recruitment> findAllByStatusWithRequiredSkills(RecruitmentStatus status);
}
