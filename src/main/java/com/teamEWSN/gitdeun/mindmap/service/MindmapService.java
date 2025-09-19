package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.dto.*;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmap.util.MindmapGraphCache;
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

import java.util.List;

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
    private final MindmapGraphCache mindmapGraphCache;

    // FastAPI 분석 결과를 받아 마인드맵을 생성하고 DB에 저장 (단일 트랜잭션)
    @Transactional
    public Mindmap saveMindmapFromAnalysis(AnalysisResultDto analysisResult, String repoUrl, String prompt, Long userId) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        // 1. 저장소 정보 처리 (기존에 있으면 업데이트, 없으면 생성)
        Repo repo = processRepository(repoUrl, analysisResult);

        // 2. AI가 생성한 제목 또는 기본 제목 결정
        String title = determineTitle(analysisResult, user);

        // 3. 마인드맵 엔티티 생성 및 저장
        Mindmap mindmap = Mindmap.builder()
            .repo(repo)
            .user(user)
            .branch(repo.getDefaultBranch())
            .title(title)
            .build();
        mindmapRepository.save(mindmap);

        // 4. 초기 프롬프트 히스토리 생성 (프롬프트가 있는 경우)
        if (StringUtils.hasText(prompt)) {
            promptHistoryService.createInitialPromptHistory(mindmap, prompt, title);
        }

        // 5. 마인드맵 소유자 멤버로 등록
        mindmapMemberRepository.save(MindmapMember.of(mindmap, user, MindmapRole.OWNER));

        // 6. 방문 기록 생성
        visitHistoryService.createVisitHistory(user, mindmap);

        log.info("마인드맵 DB 저장 완료 - ID: {}, 제목: {}", mindmap.getId(), title);
        return mindmap;
    }

    // 마인드맵 상세 정보 조회
    @Transactional(readOnly = true)
    public MindmapDetailResponseDto getMindmap(Long mapId, Long userId, String authHeader) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 캐싱된 그래프 데이터 조회
        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(
            mindmap.getRepo().getGithubRepoUrl(),
            authHeader
        );

        return mindmapMapper.toDetailResponseDto(mindmap, graphData);
    }

    //마인드맵 제목 수정
    @Transactional
    public MindmapDetailResponseDto updateMindmapTitle(Long mapId, Long userId, MindmapTitleUpdateDto req) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        mindmap.updateTitle(req.getTitle());

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
        mindmapSseService.broadcastTitleChanged(mapId, req.getTitle());

        log.info("마인드맵 제목 수정 완료 - ID: {}, 새 제목: {}", mapId, req.getTitle());
        return responseDto;
    }

    // 분석 결과를 바탕으로 기존 마인드맵을 업데이트 (새로고침)
    @Transactional
    public MindmapDetailResponseDto updateMindmapFromAnalysis(Long mapId, String authHeader, AnalysisResultDto analysisResult) {
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        mindmap.getRepo().updateWithAnalysis(analysisResult);

        // 새로고침 시 그래프 캐시 무효화
        mindmapGraphCache.evictCache(mindmap.getRepo().getGithubRepoUrl());

        MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(
            mindmap.getRepo().getGithubRepoUrl(),
            authHeader
        );

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap, graphData);
        mindmapSseService.broadcastUpdate(mapId, responseDto);

        log.info("마인드맵 새로고침 DB 업데이트 완료 - ID: {}", mapId);
        return responseDto;
    }

    /**
     * 마인드맵 소프트 삭제 (DB 작업만 수행)
     * @return 비동기 후처리를 위해 관련 Repo 엔티티 반환
     */
    @Transactional
    public Repo deleteMindmap(Long mapId, Long userId) {
        if (!mindmapAuthService.isOwner(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // 삭제 시 관련 캐시도 무효화
        mindmapGraphCache.evictCache(mindmap.getRepo().getGithubRepoUrl());

        mindmap.softDelete();
        log.info("마인드맵 소프트 삭제 완료 (DB) - ID: {}", mapId);

        return mindmap.getRepo(); // 후처리를 위해 Repo 반환
    }

    /**
     * TODO: Webhook을 통한 마인드맵 업데이트
     */
    @Transactional
    public void updateMindmapFromWebhook(WebhookUpdateDto dto, String authHeader) {
        Repo repo = repoRepository.findByGithubRepoUrl(dto.getRepoUrl())
            .orElseThrow(() -> new GlobalException(ErrorCode.REPO_NOT_FOUND_BY_URL));

        List<Mindmap> mindmapsToUpdate = repo.getMindmaps().stream()
            .filter(mindmap -> !mindmap.isDeleted())
            .toList();

        repo.updateWithWebhookData(dto);

        // Webhook 업데이트 시 관련 캐시 무효화
        mindmapGraphCache.evictCache(repo.getGithubRepoUrl());

        for (Mindmap mindmap : mindmapsToUpdate) {
            // 새로운 그래프 데이터로 업데이트된 응답 생성
            MindmapGraphResponseDto graphData = mindmapGraphCache.getGraphWithHybridCache(
                repo.getGithubRepoUrl(),
                authHeader
            );

            MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap, graphData);
            mindmapSseService.broadcastUpdate(mindmap.getId(), responseDto);
            log.info("Webhook으로 마인드맵 ID {} 업데이트 및 SSE 전송 완료", mindmap.getId());
        }
    }

    // === Private Helper Methods ===

    private Repo processRepository(String repoUrl, AnalysisResultDto analysisResult) {
        return repoRepository.findByGithubRepoUrl(repoUrl)
            .map(repo -> {
                repo.updateWithAnalysis(analysisResult);
                return repo;
            })
            .orElseGet(() -> {
                Repo newRepo = Repo.builder()
                    .githubRepoUrl(repoUrl)
                    .defaultBranch(analysisResult.getDefaultBranch())
                    .githubLastUpdatedAt(analysisResult.getLastCommit())
                    .build();
                return repoRepository.save(newRepo);
            });
    }

    private String determineTitle(AnalysisResultDto analysisResult, User user) {
        if (StringUtils.hasText(analysisResult.getTitle())) {
            return analysisResult.getTitle();
        }
        long userMindmapCount = mindmapRepository.countByUserAndDeletedAtIsNull(user);
        return "마인드맵 " + (userMindmapCount + 1);
    }

}