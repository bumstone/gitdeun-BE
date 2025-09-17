package com.teamEWSN.gitdeun.mindmap.service;


import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.request.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.request.ValidatedMindmapRequest;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.mapper.MindmapMapper;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.util.MindmapRequestValidator;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.notification.service.NotificationService;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.repo.repository.RepoRepository;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.visithistory.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 수정된 마인드맵 서비스 - Spring Boot는 조율자 역할만
 *
 * 역할 분담:
 * 1. Spring Boot: 요청 검증 → FastAPI 호출 → 결과 저장 → 알림
 * 2. FastAPI: 실제 AI 분석 + 청크 처리 + 병합 + 제목 생성
 *
 * 비유:
 * Spring Boot = 프로젝트 매니저 (요구사항 정리, 결과 관리)
 * FastAPI = 전문 개발팀 (실제 코딩 작업)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapAsyncService {
    private final MindmapRequestValidator validator;
    private final FastApiClient fastApiClient;
    private final MindmapRepository mindmapRepository;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final RepoRepository repoRepository;
    private final UserRepository userRepository;
    private final MindmapService mindmapService;
    private final VisitHistoryService visitHistoryService;
    private final NotificationService notificationService;
    private final MindmapMapper mindmapMapper;

    /**
     * 비동기 마인드맵 조율 및 생성
     */
    @Async("mindmapExecutor")
    public CompletableFuture<MindmapResponseDto> createMindmap(
        MindmapCreateRequestDto request, Long userId, String authHeader) {

        return CompletableFuture
            .supplyAsync(() -> {
                // 1단계: 유효성 검사
                log.info("마인드맵 생성 요청 검증 - 사용자: {}, 저장소: {}", userId, request.getRepoUrl());
                return validator.validateAndProcess(request.getRepoUrl(), request.getPrompt(), userId);
            })
            .thenCompose(validated -> {
                // 2단계: FastAPI에 분석 요청 (청크 처리 + 병합 모두 FastAPI에서)
                return requestAnalysisFromFastAPI(validated, authHeader);
            })
            .thenCompose(analysisResult -> {
                // 3단계: 완성된 결과를 MySQL에 저장
                return mindmapService.saveCompletedMindmap(analysisResult, userId);
            })
            .thenApply(mindmap -> {
                // 4단계: 후처리 및 성공 알림
                return handleSuccessAndNotify(mindmap, userId);
            })
            .exceptionally(throwable -> {
                // 실패 처리
                return handleFailureAndNotify(throwable, userId, request.getRepoUrl());
            });
    }

    /**
     * FastAPI에 분석 요청 - 모든 AI 작업은 여기서
     *
     * FastAPI에서 수행하는 작업:
     * 1. GitHub 저장소 다운로드
     * 2. 청크 단위로 분할
     * 3. 각 청크별 Gemini API 호출
     * 4. 결과 병합해서 최종 마인드맵 구조 생성
     * 5. AI 제목 생성
     * 6. ArangoDB에 저장
     * 7. 완성된 결과 반환
     */
    private CompletableFuture<AnalysisResultDto> requestAnalysisFromFastAPI(
        ValidatedMindmapRequest validated, String authHeader) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String repoUrl = validated.getNormalizedRepositoryUrl();
                String prompt = validated.getProcessedPrompt();

                log.info("FastAPI 분석 요청 - 저장소: {}, 프롬프트 여부: {}",
                    repoUrl, prompt != null);

                AnalysisResultDto result;
                if (prompt != null) {
                    result = fastApiClient.analyzeWithPrompt(repoUrl, prompt, authHeader);
                } else {
                    result = fastApiClient.analyzeDefault(repoUrl, authHeader);
                }

                log.info("FastAPI 분석 완료 - 제목: {}, 맵데이터 크기: {}",
                    result.getTitle(),
                    result.getMapData() != null ? result.getMapData().length() : 0);

                return result;

            } catch (Exception e) {
                log.error("FastAPI 분석 실패: {}", e.getMessage(), e);
                throw new GlobalException(ErrorCode.MINDMAP_CREATION_FAILED, "AI 분석 처리 실패: " + e.getMessage());
            }
        });
    }


    /**
     * 성공 후처리 및 알림
     */
    private MindmapResponseDto handleSuccessAndNotify(Mindmap mindmap, Long userId) {
        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

            // 방문 기록 생성
            visitHistoryService.createVisitHistory(user, mindmap);

            // 성공 알림 전송
            notificationService.createAndSendNotification(
                user,
                NotificationType.SYSTEM_UPDATE,
                "마인드맵 '" + mindmap.getTitle() + "' 생성이 완료되었습니다.",
                mindmap.getId(),
                null
            );

            log.info("마인드맵 생성 완전 완료 - ID: {}, 사용자: {}", mindmap.getId(), userId);
            return mindmapMapper.toResponseDto(mindmap);

        } catch (Exception e) {
            log.error("성공 후처리 실패: {}", e.getMessage());
            // 실패해도 마인드맵은 이미 생성됨
            return mindmapMapper.toResponseDto(mindmap);
        }
    }

    /**
     * 실패 처리 및 알림
     */
    private MindmapResponseDto handleFailureAndNotify(Throwable throwable, Long userId, String repoUrl) {
        log.error("마인드맵 생성 실패 - 사용자: {}, 저장소: {}", userId, repoUrl, throwable);

        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId).orElse(null);
            if (user != null) {
                // 실패 알림 전송
                String errorMessage = "마인드맵 생성에 실패했습니다: " + getSimplifiedErrorMessage(throwable);
                notificationService.createAndSendNotification(
                    user,
                    NotificationType.SYSTEM_UPDATE,
                    errorMessage,
                    null,
                    null
                );
            }
        } catch (Exception e) {
            log.error("실패 알림 전송 실패: {}", e.getMessage());
        }

        // 원래 예외 다시 던지기
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        } else {
            throw new GlobalException(ErrorCode.MINDMAP_CREATION_FAILED, throwable.getMessage());
        }
    }

    // === Helper Methods ===

    private Repo processRepositoryMetadata(AnalysisResultDto analysisResult) {
        // 실제로는 분석 결과에서 저장소 URL을 추출해야 함
        // 현재는 간단히 처리
        String repoUrl = ""; // analysisResult에서 추출 필요

        Optional<Repo> existingRepo = repoRepository.findByGithubRepoUrl(repoUrl);
        if (existingRepo.isPresent()) {
            Repo repo = existingRepo.get();
            // 메타데이터 업데이트
            if (analysisResult.getGithubLastUpdatedAt() != null) {
                repo.updateLastCommitTime(analysisResult.getGithubLastUpdatedAt());
            }
            if (analysisResult.getDefaultBranch() != null) {
                repo.updateDefaultBranch(analysisResult.getDefaultBranch());
            }
            return repoRepository.save(repo);
        } else {
            return repoRepository.save(
                Repo.builder()
                    .githubRepoUrl(repoUrl)
                    .defaultBranch(analysisResult.getDefaultBranch())
                    .githubLastUpdatedAt(analysisResult.getGithubLastUpdatedAt())
                    .build()
            );
        }
    }

    private String getSimplifiedErrorMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null) return "알 수 없는 오류";

        if (message.contains("timeout")) return "처리 시간 초과";
        if (message.contains("404")) return "저장소를 찾을 수 없음";
        if (message.contains("403")) return "저장소 접근 권한 없음";
        if (message.contains("AI")) return "AI 분석 중 오류";

        return "처리 중 오류 발생";
    }
}
