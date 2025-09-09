package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import com.teamEWSN.gitdeun.common.webhook.dto.WebhookUpdateDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapCreateRequestDto;
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

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapService {

    private final VisitHistoryService visitHistoryService;
    private final RepoService repoService;
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

        // 1. GitHub Repo 정보 FastAPI를 통해 저장 및 분석 요청
        fastApiClient.saveRepoInfo(req.getRepoUrl(), authorizationHeader);
        fastApiClient.fetchRepo(req.getRepoUrl(), authorizationHeader);
        AnalysisResultDto analysisResult = fastApiClient.analyzeWithAi(req.getRepoUrl(), req.getPrompt(), req.getType(), authorizationHeader);

        Repo repo = repoService.createOrUpdate(req.getRepoUrl(), analysisResult);
        repoRepository.save(repo);

        String field = determineField(req, user);

        // 2. MySQL에 마인드맵 엔티티 저장
        Mindmap mindmap = Mindmap.builder()
            .repo(repo)
            .user(user)
            .prompt(req.getPrompt())
            .branch(analysisResult.getDefaultBranch())
            .type(req.getType())
            .field(field)
            .mapData(analysisResult.getMapData()) // 초기 분석 데이터 저장
            .build();

        mindmapRepository.save(mindmap);

        // 3. 마인드맵 소유자 등록
        mindmapMemberRepository.save(
            MindmapMember.of(mindmap, user, MindmapRole.OWNER)
        );

        // 4. 방문 기록 생성
        visitHistoryService.createVisitHistory(user, mindmap);

        return mindmapMapper.toResponseDto(mindmap);
    }

    private String determineField(MindmapCreateRequestDto req, User user) {
        if (req.getType() == MindmapType.DEV) {
            return "개발용";
        } else {
            if (req.getField() != null && !req.getField().isEmpty()) {
                return req.getField();
            } else {
                long nextSeq = findNextCheckSequence(user);
                return "확인용 (" + nextSeq + ")";
            }
        }
    }

    /**
     * 특정 사용자의 "확인용 (n)" 다음 시퀀스 번호를 찾습니다.
     */
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

        syncWithArangoDB(mindmap, authorizationHeader);

        return mindmapMapper.toDetailResponseDto(mindmap);
    }

    /**
     * 마인드맵 새로고침 - ArangoDB와 완전 동기화
     */

    @Transactional
    public MindmapDetailResponseDto refreshMindmap(Long mapId, Long userId, String authorizationHeader) {
        Mindmap mindmap = mindmapRepository.findById(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        if (!mindmap.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        AnalysisResultDto dto = fastApiClient.analyzeWithAi(
            mindmap.getRepo().getGithubRepoUrl(),
            mindmap.getPrompt(),
            mindmap.getType(),
            authorizationHeader
        );

        mindmap.getRepo().updateWithAnalysis(dto);
        mindmap.updateMapData(dto.getMapData());

        MindmapDetailResponseDto responseDto = mindmapMapper.toDetailResponseDto(mindmap);
        mindmapSseService.broadcastUpdate(mapId, responseDto);
        return responseDto;
    }

    /**
     * ArangoDB와 동기화하여 최신 마인드맵 데이터를 가져옴
     */
    private void syncWithArangoDB(Mindmap mindmap, String authHeader) {
        try {
            MindmapGraphDto graphData = fastApiClient.getMindmapGraph(
                mindmap.getRepo().getGithubRepoUrl(),
                authHeader
            );

            if (graphData != null) {
                String arangoMapData = objectMapper.writeValueAsString(graphData);
                if (!arangoMapData.equals(mindmap.getMapData())) {
                    mindmap.updateMapData(arangoMapData);
                    log.info("마인드맵 동기화 완료: {}", mindmap.getId());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("마인드맵 데이터 JSON 변환 실패: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("ArangoDB 동기화 실패, 기존 데이터 유지: {}", e.getMessage());
        }
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
            log.info("ArangoDB에서 마인드맵 데이터 삭제 완료: {}", mindmap.getRepo().getGithubRepoUrl());
        } catch (Exception e) {
            log.error("ArangoDB 데이터 삭제 실패, MySQL 삭제는 계속 진행: {}", e.getMessage());
        }

        mindmapRepository.delete(mindmap);
        log.info("마인드맵 ID {} 삭제 완료", mapId);
    }

}