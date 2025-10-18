package com.teamEWSN.gitdeun.recruitment.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.teamEWSN.gitdeun.recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.recruitment.entity.RecruitmentStatus;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.teamEWSN.gitdeun.recruitment.entity.QRecruitment.recruitment;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RecruitmentRepositoryImpl implements RecruitmentCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Recruitment> searchRecruitments(
        String keyword, RecruitmentStatus status, List<RecruitmentField> fields, List<DeveloperSkill> languages, Pageable pageable
    ) {
        // 키워드 전처리 후 Full-Text Search 활용 여부 확인
        String processed = preprocessKeyword(keyword);

        // 키워드 없으면: 키워드 조건 없이 status/fields/languages 으로 페이지 조회
        boolean hasKeyword = (processed != null);
        BooleanExpression keywordExpr = null;
        boolean useFullTextSearch = false;

        if (hasKeyword) {
            if (processed.length() == 1) {
                keywordExpr = titleExactMatch(processed); // 한 글자: 완전 일치 검색
            } else {
                useFullTextSearch = isFullTextSearchAvailable(processed);
                keywordExpr = useFullTextSearch ? titleFullTextSearch(processed) // 두 글자 이상: FTS 또는 Contains
                    : fallbackContains(processed);
            }
        }


        // 엔티티 자체 데이터 조회
        // id 페이지닝
        List<Long> ids = queryFactory.select(recruitment.id).distinct()
            .from(recruitment)
            .where(keywordExpr, statusEq(status), fieldOrFilter(fields), languageOrFilter(languages))
            .orderBy(useFullTextSearch ? scoreOrder(processed) : recruitment.id.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        if (ids.isEmpty()) return Page.empty(pageable);

        // ID 목록으로 전체 데이터 조회
        String idCsv = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Recruitment> content = queryFactory.selectFrom(recruitment)
            .where(recruitment.id.in(ids))
            .orderBy(Expressions.numberTemplate(Integer.class,
                "FIELD({0}, " + idCsv + ")", recruitment.id).asc())
            .fetch();

        // 전체 카운트 조회
        Long total = queryFactory.select(recruitment.id.countDistinct())
            .from(recruitment)
            .where(keywordExpr, statusEq(status), fieldOrFilter(fields), languageOrFilter(languages))
            .fetchOne();

        return new PageImpl<>(content, pageable, total != null ? total : 0L);
    }

    // 키워드 전처리
    private String preprocessKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) return null;

        // 유니코드 정규화
        String s = java.text.Normalizer.normalize(keyword, java.text.Normalizer.Form.NFKC);

        // 제어문자 제거: NULL(0x00), Ctrl-Z(0x1A), 그 외 제어문자(탭/개행 제외)
        s = s.replace("\u0000", "").replace("\u001A", "");
        s = s.replaceAll("[\\p{Cntrl}&&[^\\t\\n\\r]]", "");

        // 공백 정규화
        s = s.trim().replaceAll("\\s+", " ");

        // 허용 문자만 남기기 (한글, 영문, 숫자, 공백, + - * " ( ) )
        s = s.replaceAll("[^가-힣A-Za-z0-9\\s\\+\\-\\*\\\"\\(\\)]", "");

        // 따옴표 균형 보정
        int quoteCount = s.length() - s.replace("\"", "").length();
        if ((quoteCount % 2) != 0) {
            int last = s.lastIndexOf('"');
            if (last >= 0) s = s.substring(0, last) + s.substring(last + 1);
        }

        // 길이 제한
        if (s.isEmpty()) return null;
        if (s.length() > 30) s = s.substring(0, 30);

        return s;
    }

    // 제목 완전 일치 검색(한글자용)
    private BooleanExpression titleExactMatch(String keyword) {
        return recruitment.title.eq(keyword);
    }

    // Full Text Search 조건 (전처리 이후 동작)
    private boolean isFullTextSearchAvailable(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return false;
        }
        return keyword.split("\\s+").length <= 5;
    }

    /**
     * MySQL Full-Text Search를 사용한 검색
     * MATCH ... AGAINST 구문 활용
     * WHERE 절: 점수 > 0 판단식
     */
    private BooleanExpression titleFullTextSearch(String keyword) {
        String booleanQuery = buildBooleanQuery(keyword); // 기존 전처리/토크나이저 사용
        return Expressions.booleanTemplate(
            "function('match_against_boolean', {0}, {1}, {2}) > 0",
            recruitment.title, recruitment.content, booleanQuery
        );
    }

    /**
     * 기본 부분 문자열 검색
     */
    private BooleanExpression fallbackContains(String keyword) {
        String k = keyword.trim();
        return recruitment.title.containsIgnoreCase(k)
            .or(recruitment.content.containsIgnoreCase(k));
    }

    // ORDER BY 절: 점수 desc
    private OrderSpecifier<Double> scoreOrder(String keyword) {
        return Expressions.numberTemplate(Double.class,
                "function('match_against_boolean', {0}, {1}, {2})",
                recruitment.title, recruitment.content, buildBooleanQuery(keyword))
            .desc();
    }

    // 따옴표 보존 토크나이저 + 빌더
    private static final Pattern TOKEN_PATTERN = Pattern.compile("\"([^\"]+)\"|(\\S+)");
    private List<String> tokenizeRespectingQuotes(String s) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN_PATTERN.matcher(s);
        while (m.find()) {
            String phrase = m.group(1);
            String single = m.group(2);
            if (phrase != null) tokens.add("\"" + phrase + "\"");
            else if (single != null) tokens.add(single);
        }
        return tokens;
    }
    private String buildBooleanQuery(String keyword) {
        List<String> parts = tokenizeRespectingQuotes(keyword.trim());
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isBlank()) continue;

            // 와일드카드 과다 방지: 중간의 **** → * 로 축약, 토큰 중간 *는 허용
            String normalized = p.replaceAll("\\*{2,}", "*");

            boolean hasOpPrefix = p.startsWith("+") || p.startsWith("-") || p.startsWith("(");
            boolean isPhrase = normalized.startsWith("\"") && normalized.endsWith("\"");

            // 단독 "*" 같은 노이즈 토큰 차단
            if ("*".equals(normalized)) continue;

            if (hasOpPrefix || isPhrase) sb.append(normalized).append(' ');
            else sb.append('+').append(normalized).append(' ');
        }
        return sb.toString().trim();
    }


    private BooleanExpression statusEq(RecruitmentStatus status) {
        return status != null ? recruitment.status.eq(status) : null;
    }

    private BooleanExpression fieldOrFilter(List<RecruitmentField> fields) {
        return CollectionUtils.isEmpty(fields)
            ? null
            : recruitment.fieldTags.any().in(fields);
    }

    private BooleanExpression languageOrFilter(List<DeveloperSkill> languages) {
        return CollectionUtils.isEmpty(languages)
            ? null
            : recruitment.languageTags.any().in(languages);
    }
}