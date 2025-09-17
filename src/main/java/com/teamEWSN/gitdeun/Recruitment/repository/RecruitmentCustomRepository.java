package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RecruitmentCustomRepository {
    Page<Recruitment> searchRecruitments(
        String keyword,
        RecruitmentStatus status,
        List<RecruitmentField> fields,
        Pageable pageable
    );
}
