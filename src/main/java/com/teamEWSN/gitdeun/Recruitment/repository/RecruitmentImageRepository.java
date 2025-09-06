package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecruitmentImageRepository extends JpaRepository<RecruitmentImage, Long> {
    List<RecruitmentImage> findByRecruitmentAndDeletedAtIsNull(Recruitment recruitment);

    List<RecruitmentImage> findByRecruitmentIdAndDeletedAtIsNull(Long recruitmentId);

}
