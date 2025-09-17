package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.dto.*;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapService {

    private final VisitHistoryService visitHistoryService;
    private final MindmapSseService mindmapSseService;
    private final MindmapAuthService mindmapAuthService;
    private final PromptHistoryService promptHistoryService;
    private final MindmapMapper mindmapMapper;
    private final MindmapRepository mindmapRepository;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final FastApiClient fastApiClient;
    private final ObjectMapper objectMapper;

    // 마인드맵 생성
    @Transactional
    public MindmapResponseDto createMindmap(MindmapCreateRequestDto req, Long userId, String authorizationHeader) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        String normalizedUrl = normalizeRepoUrl(req.getRepoUrl());

        // 1. Repository 처리
        Repo repo = processRepository(normalizedUrl, authorizationHeader);

        // 2. FastAPI를 통해 분석 수행 및 AI 생성 제목과 맵 데이터 획득
        AnalysisResultDto analysisResult = generateMapDataWithAnalysis(normalizedUrl, req.getPrompt(), authorizationHeader);

        // 3. AI가 생성한 제목 사용, 실패 시 기본 제목
        String title = determineAIGeneratedTitle(analysisResult, user);

        // 4. 마인드맵 엔티티 생성
        Mindmap mindmap = Mindmap.builder()
            .repo(repo)
            .user(user)
            .branch(repo.getDefaultBranch())
            .title(title)
            .mapData(analysisResult.getMapData())
            .build();

        mindmapRepository.save(mindmap);

        // 5. 초기 프롬프트 히스토리 생성 (프롬프트가 있는 경우)
        if (StringUtils.hasText(req.getPrompt())) {
            promptHistoryService.createInitialPromptHistory(mindmap, req.getPrompt(), analysisResult.getMapData(),
                analysisResult.getTitle());
        }

        // 6. 소유자 등록 및 방문 기록
        mindmapMemberRepository.save(MindmapMember.of(mindmap, user, MindmapRole.OWNER));
        visitHistoryService.createVisitHistory(user, mindmap);

        log.info("마인드맵 생성 완료 - ID: {}, AI 생성 제목: {}", mindmap.getId(), title);
        return mindmapMapper.toResponseDto(mindmap);
    }

    /**
     * 마인드맵 상세 정보 조회
     */
    @Transactional
    public MindmapDetailResponseDto getMindmap(Long mapId, Long userId, String authorizationHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        syncWithArangoDB(mindmap, authorizationHeader);

        return mindmapMapper.toDetailResponseDto(mindmap);
    }

    /**
     * 마인드맵 제목 수정
     */
    @Transactional
    public MindmapDetailResponseDto updateMindmapTitle(Long mapId, Long userId, MindmapTitleUpdateDto req) {

        // EDIT 권한 필요
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        mindmap.updateTitle(req.getTitle());

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);

        // 제목 변경만 별도 브로드캐스트
        mindmapSseService.broadcastTitleChanged(mapId, req.getTitle());

        log.info("마인드맵 제목 수정 완료 - ID: {}, 새 제목: {}", mapId, req.getTitle());
        return responseDto;
    }

    /**
     * 마인드맵 새로고침
     */
    @Transactional
    public MindmapDetailResponseDto refreshMindmap(Long mapId, Long userId, String authorizationHeader) {

        // 마인드맵 멤버 확인
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();

        try {
            // 저장소 최신 설정
            fastApiClient.saveRepoInfo(repoUrl, authorizationHeader);
            fastApiClient.fetchRepo(repoUrl, authorizationHeader);

            // 현재 적용된 프롬프트 확인
            PromptHistory appliedPrompt = mindmap.getAppliedPromptHistory();
            AnalysisResultDto analysisResult;

            if (appliedPrompt != null && StringUtils.hasText(appliedPrompt.getPrompt())) {
                analysisResult = fastApiClient.analyzeWithPrompt(repoUrl, appliedPrompt.getPrompt(), authorizationHeader);
            } else {
                analysisResult = fastApiClient.analyzeDefault(repoUrl, authorizationHeader);
            }

            mindmap.getRepo().updateWithAnalysis(analysisResult);
            mindmap.updateMapData(analysisResult.getMapData());

            MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
            mindmapSseService.broadcastUpdate(mapId, responseDto);

            return responseDto;

        } catch (Exception e) {
            log.error("마인드맵 새로고침 실패: {}", e.getMessage(), e);
            throw new RuntimeException("마인드맵 새로고침 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 마인드맵 소프트 삭제
     */
    @Transactional
    public void deleteMindmap(Long mapId, Long userId, String authorizationHeader) {

        // Owner만 가능
        if (!mindmapAuthService.isOwner(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        try {
            fastApiClient.deleteMindmapData(mindmap.getRepo().getGithubRepoUrl(), authorizationHeader);
            log.info("ArangoDB 데이터 삭제 완료: {}", mindmap.getRepo().getGithubRepoUrl());
        } catch (Exception e) {
            log.error("ArangoDB 데이터 삭제 실패, 마인드맵 소프트 삭제는 계속 진행: {}", e.getMessage());
        }

        // 소프트 삭제 수행
        mindmap.softDelete();
        log.info("마인드맵 소프트 삭제 완료: {}", mapId);
    }

    /**
     * Webhook을 통한 마인드맵 업데이트
     */
    @Transactional
    public void updateMindmapFromWebhook(WebhookUpdateDto dto, String authorizationHeader) {
        Repo repo = repoRepository.findByGithubRepoUrl(dto.getRepoUrl())
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_URL));

        // 삭제되지 않은 마인드맵만 업데이트
        List<Mindmap> mindmapsToUpdate = repo.getMindmaps().stream()
            .filter(mindmap -> !mindmap.isDeleted())
            .toList();

        repo.updateWithWebhookData(dto);

        for (Mindmap mindmap : mindmapsToUpdate) {
            mindmap.updateMapData(dto.getMapData());
            MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
            mindmapSseService.broadcastUpdate(mindmap.getId(), responseDto);

            log.info("Webhook으로 마인드맵 ID {} 업데이트 및 SSE 전송 완료", mindmap.getId());
        }
    }

// === Private Helper Methods ===

    private Repo processRepository(String repoUrl, String authHeader) {
        Optional<Repo> existingRepo = repoRepository.findByGithubRepoUrl(repoUrl);
        Repo repo;

        if (existingRepo.isPresent()) {
            repo = existingRepo.get();
            log.info("기존 저장소 발견: {}", repoUrl);

            if (shouldUpdateRepo(repo, authHeader)) {
                log.info("저장소 업데이트 필요: {}", repoUrl);
                fastApiClient.saveRepoInfo(repoUrl, authHeader);
                fastApiClient.fetchRepo(repoUrl, authHeader);
            }
        } else {
            log.info("새 저장소: {}", repoUrl);
            repo = Repo.builder().githubRepoUrl(repoUrl).build();
            fastApiClient.saveRepoInfo(repoUrl, authHeader);
            fastApiClient.fetchRepo(repoUrl, authHeader);
        }

        return repo;
    }

    /**
     * FastAPI를 통한 분석 수행 및 AI 생성 제목과 맵 데이터 획득
     */
    private AnalysisResultDto generateMapDataWithAnalysis(String repoUrl, String prompt, String authHeader) {
        try {
            AnalysisResultDto analysisResult;
            if (StringUtils.hasText(prompt)) {
                // 프롬프트가 있는 경우 - AI가 맞춤형 분석 및 제목 생성
                analysisResult = fastApiClient.analyzeWithPrompt(repoUrl, prompt, authHeader);
                log.info("프롬프트 기반 AI 분석 완료 - 생성된 제목: {}", analysisResult.getTitle());
            } else {
                // 프롬프트가 없는 경우 - 기본 분석 (제목 생성 안됨)
                analysisResult = fastApiClient.analyzeDefault(repoUrl, authHeader);
                log.info("기본 분석 완료 - AI 제목 생성 없음");
            }

            return analysisResult;
        } catch (Exception e) {
            log.error("마인드맵 데이터 생성 실패: {}", e.getMessage(), e);

            // 분석 실패 시 예외를 다시 던져서 상위에서 처리하도록 함
            // 에러 메시지는 로그와 예외로만 관리
            throw new RuntimeException("FastAPI 분석 실패: " + e.getMessage(), e);
        }
    }

    /**
     * AI 생성 제목 결정 로직
     * 1. 프롬프트 있고 AI 제목 생성 성공 → AI 제목 사용
     * 2. 프롬프트 없거나 AI 제목 생성 실패 → 자동 번호 제목
     */
    private String determineAIGeneratedTitle(AnalysisResultDto analysisResult, User user) {
        // AI가 제목을 성공적으로 생성한 경우
        if (analysisResult != null && StringUtils.hasText(analysisResult.getTitle())) {
            log.info("AI 생성 제목 사용: {}", analysisResult.getTitle());
            return analysisResult.getTitle();
        }

        // AI 제목 생성 실패 또는 프롬프트 없는 경우 → 자동 번호 제목
        long userMindmapCount = mindmapRepository.countByUserAndDeletedAtIsNull(user);
        String defaultTitle = "마인드맵 " + (userMindmapCount + 1);

        log.info("기본 제목 사용: {}", defaultTitle);
        return defaultTitle;
    }

    private boolean shouldUpdateRepo(Repo repo, String authHeader) {
        try {
            LocalDateTime githubLastCommit = fastApiClient.getRepositoryLastCommitTime(repo.getGithubRepoUrl(), authHeader);

            if (repo.getGithubLastUpdatedAt() == null) {
                return true;
            }

            return githubLastCommit.isAfter(repo.getGithubLastUpdatedAt());
        } catch (Exception e) {
            log.warn("저장소 업데이트 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private String getMapDataFromArangoDB(String repoUrl, String authHeader) {
        try {
            MindmapGraphDto graphData = fastApiClient.getMindmapGraph(repoUrl, authHeader);
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

}