package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentRequiredSkillRepository extends JpaRepository<RecruitmentRequiredSkill, Long> {
}
