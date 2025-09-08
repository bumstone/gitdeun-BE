package com.teamEWSN.gitdeun.Recruitment.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
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
    public Page<Recruitment> searchRecruitments(
        RecruitmentStatus status, List<RecruitmentField> fields, Pageable pageable
    ) {
        // 1. 데이터 조회 (엔티티 자체를 조회)
        List<Recruitment> content = queryFactory
            .selectFrom(recruitment)
            .distinct() // 중복 제거
            .leftJoin(recruitment.fieldTags).fetchJoin() // fetch join으로 N+1 문제 방지
            .leftJoin(recruitment.languageTags).fetchJoin() // fetch join
            .where(statusEq(status), fieldIn(fields))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .orderBy(recruitment.id.desc())
            .fetch();

        // 2. 전체 카운트 조회 (조건은 동일하게)
        JPAQuery<Long> countQuery = queryFactory
            .select(recruitment.count())
            .from(recruitment)
            .where(statusEq(status), fieldIn(fields));

        Long total = countQuery.fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }


    private BooleanExpression statusEq(RecruitmentStatus status) {
        return status != null ? recruitment.status.eq(status) : null;
    }

    private BooleanExpression fieldIn(List<RecruitmentField> fields) {
        return !CollectionUtils.isEmpty(fields) ? recruitment.fieldTags.any().in(fields) : null;
    }
}