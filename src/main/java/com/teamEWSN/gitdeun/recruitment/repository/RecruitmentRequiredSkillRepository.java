package com.teamEWSN.gitdeun.recruitment.repository;

import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentRequiredSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentRequiredSkillRepository extends JpaRepository<RecruitmentRequiredSkill, Long> {
}
