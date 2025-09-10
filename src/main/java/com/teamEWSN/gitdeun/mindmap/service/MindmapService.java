package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreationResultDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
import com.teamEWSN.gitdeun.repo.service.RepoService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.visithistory.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapService {

    private final VisitHistoryService visitHistoryService;
    private final MindmapSseService mindmapSseService;
    private final MindmapAuthService mindmapAuthService;
    private final MindmapMapper mindmapMapper;
    private final MindmapRepository mindmapRepository;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;    // JSON 직렬화


    @Transactional
    public MindmapResponseDto createMindmapFromAnalysis(MindmapCreateRequestDto req, Long userId, String authorizationHeader) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 입력 검증
        if (req.getType() == MindmapType.DEV && !StringUtils.hasText(req.getPrompt())) {
            throw new IllegalArgumentException("DEV 타입 마인드맵은 프롬프트가 필수입니다.");
        }

        String normalizedUrl = normalizeRepoUrl(req.getRepoUrl());

        // 타입별 처리
        MindmapCreationResultDto result;
        if (req.getType() == MindmapType.DEV) {
            result = createDevMindmap(normalizedUrl, req.getPrompt(), authorizationHeader);
        } else {
            result = createCheckMindmap(normalizedUrl, req.getField(), user, authorizationHeader);
        }

        // 마인드맵 엔티티 생성
        Mindmap mindmap = Mindmap.builder()
            .repo(result.getRepo())
            .user(user)
            .prompt(req.getType() == MindmapType.DEV ? req.getPrompt() : null)
            .branch(result.getRepo().getDefaultBranch())
            .type(req.getType())
            .field(result.getField())
            .mapData(result.getMapData())
            .build();

        mindmapRepository.save(mindmap);

        // 소유자 등록 및 방문 기록
        mindmapMemberRepository.save(MindmapMember.of(mindmap, user, MindmapRole.OWNER));
        visitHistoryService.createVisitHistory(user, mindmap);

        log.info("마인드맵 생성 완료 - ID: {}, Type: {}, Field: {}", mindmap.getId(), req.getType(), result.getField());
        return mindmapMapper.toResponseDto(mindmap);
    }

    /**
     * 마인드맵 상세 정보 조회 - ArangoDB와 동기화된 최신 데이터 반환
     */
    @Transactional
    public MindmapDetailResponseDto getMindmap(Long mapId, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // ArangoDB와 동기화하여 최신 데이터 반영
        syncWithArangoDB(mindmap, authorizationHeader);

        return mindmapMapper.toDetailResponseDto(mindmap);
    }

    /**
     * 마인드맵 새로고침
     */
    @Transactional
    public MindmapDetailResponseDto refreshMindmap(Long mapId, Long userId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();
        String newMapData;

        try {
            // 저장소 최신 설정
            fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
            fastApiClient.fetchRepo(repoUrl, authorizationHeader);

            // 타입별 재분석
            if (mindmap.getType() == MindmapType.DEV) {
                AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(repoUrl, mindmap.getPrompt(), MindmapType.DEV, authorizationHeader);
                mindmap.getRepo().updateWithAnalysis(analysisResult);
                newMapData = analysisResult.getMapData();
            } else {
                AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(repoUrl, null, MindmapType.CHECK, authorizationHeader);
                mindmap.getRepo().updateWithAnalysis(analysisResult);
                newMapData = analysisResult.getMapData();
            }

            mindmap.updateMapData(newMapData);

        } catch (Exception e) {
            log.error("마인드맵 새로고침 실패: {}", e.getMessage(), e);
            throw new RuntimeException("마인드맵 새로고침 중 오류가 발생했습니다: " + e.getMessage());
        }

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
        mindmapSseService.broadcastUpdate(mapId, responseDto);
        return responseDto;
    }

    // TODO: webhook을 통한 업데이트
    @Transactional
    public void updateMindmapFromWebhook(WebhookUpdateDto dto, String authorizationHeader) {
        Repo repo = repoRepository.findByGithubRepoUrl(dto.getRepoUrl())
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_URL));

        List<Mindmap> mindmapsToUpdate = repo.getMindmaps();

        repo.updateWithWebhookData(dto);

        for (Mindmap mindmap : mindmapsToUpdate) {
            mindmap.updateMapData(dto.getMapData());
            MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
            mindmapSseService.broadcastUpdate(mindmap.getId(), responseDto);

            log.info("Webhook으로 마인드맵 ID {} 업데이트 및 SSE 전송 완료", mindmap.getId());
        }
    }

    /**
     * 마인드맵 삭제 - ArangoDB 데이터도 함께 삭제
     */
    @Transactional
    public void deleteMindmap(Long mapId, Long userId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        try {
            fastApiClient.deleteMindmapData(mindmap.getRepo().getGithubRepoUrl(), authorizationHeader);
            log.info("ArangoDB 데이터 삭제 완료: {}", mindmap.getRepo().getGithubRepoUrl());
        } catch (Exception e) {
            log.error("ArangoDB 데이터 삭제 실패, MySQL 삭제는 계속 진행: {}", e.getMessage());
        }

        mindmapRepository.delete(mindmap);
        log.info("마인드맵 삭제 완료: {}", mapId);
    }


// === Private Helper Methods ===

    /**
     * DEV 타입 마인드맵 생성 - FastAPI에서 프롬프트 기반 제목도 생성
     */
    private MindmapCreationResultDto createDevMindmap(String repoUrl, String prompt, String authorizationHeader) {
        log.info("DEV 타입 마인드맵 생성 시작 - repoUrl: {}, prompt: {}", repoUrl, prompt);

        Optional<Repo> existingRepo = repoRepository.findByGithubRepoUrl(repoUrl);
        Repo repo;

        if (existingRepo.isPresent()) {
            repo = existingRepo.get();
            log.info("기존 저장소 발견 (DEV): {}", repoUrl);

            if (shouldUpdateRepo(repo, authorizationHeader)) {
                log.info("저장소 업데이트 필요 (DEV): {}", repoUrl);
                fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
                fastApiClient.fetchRepo(repoUrl, authorizationHeader);
            }
        } else {
            log.info("새 저장소 (DEV): {}", repoUrl);
            repo = Repo.builder().githubRepoUrl(repoUrl).build();
            fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
            fastApiClient.fetchRepo(repoUrl, authorizationHeader);
        }

        try {
            // DEV 전용 분석 - FastAPI에서 프롬프트를 요약한 제목까지 반환받음
            AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(repoUrl, prompt, MindmapType.DEV, authorizationHeader);
            repo.updateWithAnalysis(analysisResult);
            repoRepository.save(repo);

            // FastAPI에서 생성한 제목 사용
            return new MindmapCreationResultDto(repo, analysisResult.getMapData(), analysisResult.getField());

        } catch (Exception e) {
            log.error("DEV 분석 실패: {}", e.getMessage(), e);
            throw new RuntimeException("개발 방향성 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * CHECK 타입 마인드맵 생성
     */
    private MindmapCreationResultDto createCheckMindmap(String repoUrl, String userField, User user, String authorizationHeader) {
        log.info("CHECK 타입 마인드맵 생성 시작 - repoUrl: {}, field: {}", repoUrl, userField);

        Optional<Repo> existingRepo = repoRepository.findByGithubRepoUrl(repoUrl);
        Repo repo;
        String mapData;

        if (existingRepo.isPresent()) {
            repo = existingRepo.get();
            log.info("기존 저장소 발견 (CHECK): {}", repoUrl);

            if (shouldUpdateRepo(repo, authorizationHeader)) {
                log.info("저장소 업데이트 필요 (CHECK): {}", repoUrl);
                fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
                fastApiClient.fetchRepo(repoUrl, authorizationHeader);
                AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(repoUrl, null, MindmapType.CHECK, authorizationHeader);
                repo.updateWithAnalysis(analysisResult);
                mapData = analysisResult.getMapData();
            } else {
                log.info("저장소가 최신 상태 (CHECK), ArangoDB에서 기존 데이터 조회: {}", repoUrl);
                mapData = getMapDataFromArangoDB(repoUrl, authorizationHeader);
            }
        } else {
            log.info("새 저장소 (CHECK): {}", repoUrl);
            repo = Repo.builder().githubRepoUrl(repoUrl).build();
            fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
            fastApiClient.fetchRepo(repoUrl, authorizationHeader);
            AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(repoUrl, null, MindmapType.CHECK, authorizationHeader);
            repo.updateWithAnalysis(analysisResult);
            mapData = analysisResult.getMapData();
        }

        repoRepository.save(repo);

        // CHECK 타입 제목 결정
        String field;
        if (StringUtils.hasText(userField)) {
            field = userField;
        } else {
            long nextSeq = findNextCheckSequence(user);
            field = "확인용 (" + nextSeq + ")";
        }

        return new MindmapCreationResultDto(repo, mapData, field);
    }

    private boolean shouldUpdateRepo(Repo repo, String authorizationHeader) {
        try {
            LocalDateTime githubLastCommit = fastApiClient.getRepositoryLastCommitTime(repo.getGithubRepoUrl(), authorizationHeader);

            if (repo.getGithubLastUpdatedAt() == null) {
                return true;
            }

            return githubLastCommit.isAfter(repo.getGithubLastUpdatedAt());
        } catch (Exception e) {
            log.warn("저장소 업데이트 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private String getMapDataFromArangoDB(String repoUrl, String authorizationHeader) {
        try {
            MindmapGraphDto graphData = fastApiClient.getMindmapGraph(repoUrl, authorizationHeader);
            return graphData != null ? objectMapper.writeValueAsString(graphData) : "{}";
        } catch (Exception e) {
            log.warn("ArangoDB 데이터 조회 실패: {}", e.getMessage());
            return "{}";
        }
    }

    private void syncWithArangoDB(Mindmap mindmap, String authHeader) {
        try {
            String latestMapData = getMapDataFromArangoDB(mindmap.getRepo().getGithubRepoUrl(), authHeader);
            if (!latestMapData.equals(mindmap.getMapData())) {
                mindmap.updateMapData(latestMapData);
                log.info("마인드맵 동기화 완료: {}", mindmap.getId());
            }
        } catch (Exception e) {
            log.warn("ArangoDB 동기화 실패: {}", e.getMessage());
        }
    }

    private String normalizeRepoUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Repository URL cannot be null or empty");
        }

        return url.trim()
            .toLowerCase()
            .replaceAll("/$", "")
            .replaceAll("\\.git$", "");
    }

    private long findNextCheckSequence(User user) {
        Optional<Mindmap> lastCheckMindmap = mindmapRepository.findTopByUserAndTypeOrderByCreatedAtDesc(user);

        if (lastCheckMindmap.isEmpty()) {
            return 1;
        }

        Pattern pattern = Pattern.compile("\\((\\d+)\\)");
        Matcher matcher = pattern.matcher(lastCheckMindmap.get().getField());

        if (matcher.find()) {
            long lastSeq = Long.parseLong(matcher.group(1));
            return lastSeq + 1;
        }

        return 1;
    }
}