package com.teamEWSN.gitdeun.recruitment.repository;

import com.teamEWSN.gitdeun.recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecruitmentCustomRepository {
    Page<Recruitment> searchRecruitments(
        String keyword,
        RecruitmentStatus status,
        List<RecruitmentField> fields,
        List<DeveloperSkill> languages,
        Pageable pageable
    );
}
