package com.teamEWSN.gitdeun.Application.repository;

import com.teamEWSN.gitdeun.Application.entity.Application;
import com.teamEWSN.gitdeun.recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 특정 사용자의 활성 지원 내역만 조회
    Page<Application> findByApplicantAndActiveTrueOrderByCreatedAtDesc(User applicant, Pageable pageable);

    // 특정 공고의 활성 지원자만 조회
    Page<Application> findByRecruitmentAndActiveTrueOrderByCreatedAtDesc(Recruitment recruitment, Pageable pageable);

    // 사용자가 특정 공고에 이미 지원했는지 확인 (활성 지원만)
    boolean existsByRecruitmentAndApplicantAndActiveTrue(Recruitment recruitment, User applicant);

    // 특정 지원 조회 (지원자 본인 확인용)
    Optional<Application> findByIdAndApplicant(Long id, User applicant);

    // 특정 지원 조회 (공고 작성자 확인용)
    @Query("SELECT a FROM Application a WHERE a.id = :id AND a.recruitment.recruiter = :recruiter")
    Optional<Application> findByIdAndRecruiter(@Param("id") Long id, @Param("recruiter") User recruiter);

}
