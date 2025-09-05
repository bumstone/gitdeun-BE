package com.teamEWSN.gitdeun.Recruitment.repository;

import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentListResponseDto;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface RecruitmentCustomRepository {
    Page<RecruitmentListResponseDto> searchRecruitments(
        RecruitmentStatus status,
        List<RecruitmentField> fields,
        Pageable pageable
    );
}
