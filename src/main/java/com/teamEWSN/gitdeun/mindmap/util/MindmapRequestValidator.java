package com.teamEWSN.gitdeun.mindmap.util;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmap.dto.request.ValidatedMindmapRequest;
import com.teamEWSN.gitdeun.repo.dto.GitHubRepositoryInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * 마인드맵 생성 요청의 1단계 검증 및 전처리를 담당하는 컴포넌트
 * 기본 입력값 검증 → GitHub URL 파싱 및 정규화 → 프롬프트 및 저장소 접근성 사전 검증 → 검증된 요청 객체 반환
 */
@Slf4j
@Component
public class MindmapRequestValidator {

    // GitHub URL 패턴들
    private static final Pattern GITHUB_REPO_PATTERN = Pattern.compile(
        "^https?://github\\.com/([^/]+)/([^/]+)(?:/.*)?$", Pattern.CASE_INSENSITIVE
    );

    private static final Pattern GITHUB_SHORT_PATTERN = Pattern.compile(
        "^([^/]+)/([^/]+)$"  // "owner/repo" 형태
    );

    // 지원되는 저장소 크기 제한 (MB)
    private static final long MAX_REPO_SIZE_MB = 1000;

    // 프롬프트 길이 제한
    private static final int MAX_PROMPT_LENGTH = 2000;

    // 금지된 저장소명 패턴 (보안상 위험하거나 시스템 리소스 과다 사용)
    private static final Pattern FORBIDDEN_REPO_NAMES = Pattern.compile(
        ".*(test|example|demo|tutorial|sample).*", Pattern.CASE_INSENSITIVE
    );

    /**
     * 마인드맵 생성 요청 종합 검증
     *
     * @param repoUrl 저장소 URL (필수)
     * @param prompt  사용자 프롬프트 (선택)
     * @param userId  요청 사용자 ID
     * @return 검증된 요청 정보
     */
    public ValidatedMindmapRequest validateAndProcess(String repoUrl, String prompt, Long userId) {
        log.info("마인드맵 생성 요청 검증 시작 - 사용자: {}, URL: {}", userId, repoUrl);

        // === 1. 기본 입력값 검증 ===
        validateBasicInputs(repoUrl, userId);

        // === 2. GitHub URL 파싱 및 정규화 ===
        GitHubRepositoryInfo repoInfo = parseAndNormalizeGitHubUrl(repoUrl);

        // === 3. 프롬프트 검증 및 전처리 ===
        String processedPrompt = validateAndProcessPrompt(prompt);

        // === 4. 저장소 정책 검증 ===
        validateRepositoryPolicy(repoInfo);

        // === 5. 최종 검증 결과 반환 ===
        ValidatedMindmapRequest result = ValidatedMindmapRequest.builder()
            .repositoryInfo(repoInfo)
            .processedPrompt(processedPrompt)
            .userId(userId)
            .validatedAt(java.time.LocalDateTime.now())
            .build();

        log.info("요청 검증 완료 - 정규화된 URL: {}, 프롬프트 길이: {}",
            repoInfo.getNormalizedUrl(),
            processedPrompt != null ? processedPrompt.length() : 0);

        return result;
    }

    /**
     * 1-1. 기본 입력값 검증
     */
    private void validateBasicInputs(String repoUrl, Long userId) {
        if (userId == null || userId <= 0) {
            throw new GlobalException(ErrorCode.INVALID_USER_ID);
        }

        if (!StringUtils.hasText(repoUrl)) {
            throw new GlobalException(ErrorCode.REPOSITORY_URL_REQUIRED);
        }

        if (repoUrl.length() > 1000) {
            throw new GlobalException(ErrorCode.REPOSITORY_URL_TOO_LONG);
        }
    }

    /**
     * 1-2. GitHub URL 파싱 및 정규화
     */
    private GitHubRepositoryInfo parseAndNormalizeGitHubUrl(String rawUrl) {
        String cleanUrl = rawUrl.trim().toLowerCase();

        // HTTPS 형태 GitHub URL 파싱
        Matcher fullMatcher = GITHUB_REPO_PATTERN.matcher(cleanUrl);
        if (fullMatcher.matches()) {
            String owner = fullMatcher.group(1);
            String repo = fullMatcher.group(2);
            return createRepositoryInfo(owner, repo, cleanUrl);
        }

        // 단축 형태 "owner/repo" 파싱
        Matcher shortMatcher = GITHUB_SHORT_PATTERN.matcher(cleanUrl);
        if (shortMatcher.matches()) {
            String owner = shortMatcher.group(1);
            String repo = shortMatcher.group(2);
            String normalizedUrl = "https://github.com/" + owner + "/" + repo;
            return createRepositoryInfo(owner, repo, normalizedUrl);
        }

        // 지원하지 않는 URL 형태
        throw new GlobalException(ErrorCode.UNSUPPORTED_REPOSITORY_URL);
    }

    /**
     * 저장소 정보 객체 생성 및 추가 정규화
     */
    private GitHubRepositoryInfo createRepositoryInfo(String owner, String repo, String normalizedUrl) {
        // .git 접미사 제거
        if (repo.endsWith(".git")) {
            repo = repo.substring(0, repo.length() - 4);
        }

        // 최종 정규화된 URL
        String finalUrl = "https://github.com/" + owner + "/" + repo;

        // URL 유효성 검증 (URI 파싱)
        try {
            URI uri = new URI(finalUrl);
            if (!uri.getScheme().equals("https") || !uri.getHost().equals("github.com")) {
                throw new GlobalException(ErrorCode.INVALID_GITHUB_URL);
            }
        } catch (URISyntaxException e) {
            throw new GlobalException(ErrorCode.INVALID_REPOSITORY_URL);
        }

        return GitHubRepositoryInfo.builder()
            .owner(owner)
            .repositoryName(repo)
            .normalizedUrl(finalUrl)
            .originalUrl(normalizedUrl)
            .isOrganization(isOrganizationRepository(owner))
            .build();
    }

    /**
     * 1-3. 프롬프트 검증 및 전처리
     */
    private String validateAndProcessPrompt(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }

        String trimmed = prompt.trim();

        // 길이 검증
        if (trimmed.length() > MAX_PROMPT_LENGTH) {
            throw new GlobalException(ErrorCode.PROMPT_TOO_LONG);
        }

        // 최소 길이 검증 (의미있는 프롬프트인지)
        if (trimmed.length() < 3) {
            throw new GlobalException(ErrorCode.PROMPT_TOO_SHORT);
        }

        // 악성 패턴 검사 (SQL Injection, XSS 등)
        if (containsMaliciousPatterns(trimmed)) {
            throw new GlobalException(ErrorCode.MALICIOUS_PROMPT_DETECTED);
        }

        return trimmed;
        /*// 특수문자 정리
        return trimmed.replaceAll("[\\r\\n\\t]+", " ").trim();*/
    }

    /**
     * 1-4. 저장소 정책 검증
     */
    private void validateRepositoryPolicy(GitHubRepositoryInfo repoInfo) {
        String repoName = repoInfo.getRepositoryName();
        String owner = repoInfo.getOwner();

        // 금지된 저장소명 패턴 검사
        if (FORBIDDEN_REPO_NAMES.matcher(repoName).matches()) {
            log.warn("금지된 저장소 패턴 감지: {}/{}", owner, repoName);
            throw new GlobalException(ErrorCode.FORBIDDEN_REPOSITORY_PATTERN);
        }

        // 시스템 보호를 위한 특정 저장소 차단
        if (isSystemProtectedRepository(owner, repoName)) {
            throw new GlobalException(ErrorCode.SYSTEM_PROTECTED_REPOSITORY);
        }

        // 대용량 저장소 사전 검사 (선택적 - 실제 GitHub API 호출 필요)
        // validateRepositorySize(repoInfo); // 구현 시 GitHub API 활용
    }

    // === 유틸리티 메서드들 ===

    /**
     * 악성 패턴 검사
     */
    private boolean containsMaliciousPatterns(String input) {
        String lowerInput = input.toLowerCase();
        String[] maliciousPatterns = {
            "javascript:", "data:", "vbscript:",
            "<script", "</script>",
            "union select", "drop table", "delete from",
            "../", "..\\", "file://",
            "localhost", "127.0.0.1", "0.0.0.0"
        };

        for (String pattern : maliciousPatterns) {
            if (lowerInput.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 조직 저장소 여부 판단 (휴리스틱)
     */
    private boolean isOrganizationRepository(String owner) {
        // 일반적으로 조직명은 소문자, 개인 계정은 대소문자 혼용
        // 완벽하지 않지만 기본적인 구분 가능
        return owner.equals(owner.toLowerCase()) && !owner.contains("_");
    }

    /**
     * 시스템 보호 저장소 여부 (보안/성능상 차단해야 할 저장소들)
     */
    private boolean isSystemProtectedRepository(String owner, String repo) {
        String[] protectedOwners = {"microsoft", "google", "facebook", "apache", "kubernetes"};
        String[] protectedRepos = {"linux", "chromium", "webkit", "llvm", "gcc"};

        for (String protectedOwner : protectedOwners) {
            if (owner.equals(protectedOwner)) {
                return true;  // 대형 조직의 대용량 저장소들
            }
        }

        for (String protectedRepo : protectedRepos) {
            if (repo.equals(protectedRepo)) {
                return true;  // 알려진 대용량 시스템 저장소들
            }
        }

        return false;
    }

}
