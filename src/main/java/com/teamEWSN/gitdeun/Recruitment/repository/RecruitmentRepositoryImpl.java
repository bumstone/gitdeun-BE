package com.teamEWSN.gitdeun.Recruitment.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teamEWSN.gitdeun.Recruitment.dto.RecruitmentListResponseDto;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.teamEWSN.gitdeun.Recruitment.entity.QRecruitment.recruitment;

@Repository
@RequiredArgsConstructor
public class RecruitmentRepositoryImpl implements RecruitmentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<RecruitmentListResponseDto> searchRecruitments(
        RecruitmentStatus status, List<RecruitmentField> fields, Pageable pageable
    ) {
        List<RecruitmentListResponseDto> content = queryFactory
            .select(Projections.bean(RecruitmentListResponseDto.class,
                recruitment.id,
                recruitment.title,
                recruitment.status,
                recruitment.languageTags,
                recruitment.fieldTags,
                recruitment.startAt,
                recruitment.endAt,
                recruitment.recruitQuota
            ))
            .from(recruitment)
            .where(statusEq(status), fieldIn(fields))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(recruitment.id.desc())
            .fetch();

        Long total = queryFactory
            .select(recruitment.count())
            .from(recruitment)
            .where(statusEq(status), fieldIn(fields))
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }


    private BooleanExpression statusEq(RecruitmentStatus status) {
        return status != null ? recruitment.status.eq(status) : null;
    }

    private BooleanExpression fieldIn(List<RecruitmentField> fields) {
        return !CollectionUtils.isEmpty(fields) ? recruitment.fieldTags.any().in(fields) : null;
    }
}