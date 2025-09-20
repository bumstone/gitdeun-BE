package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.mindmap.dto.request.MindmapCreateRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.request.ValidatedMindmapRequest;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.util.MindmapRequestValidator;
import com.teamEWSN.gitdeun.notification.dto.NotificationCreateDto;
import com.teamEWSN.gitdeun.notification.entity.NotificationType;
import com.teamEWSN.gitdeun.notification.service.NotificationService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapOrchestrationService {

    private final FastApiClient fastApiClient;
    private final MindmapService mindmapService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapRequestValidator requestValidator;

    /**
     * 비동기적으로 마인드맵 생성 과정을 총괄
     */
    @Async("mindmapExecutor")
    public void createMindmap(MindmapCreateRequestDto request, Long userId, String authHeader) {
        log.info("마인드맵 생성 요청 검증 시작 - 사용자: {}", userId);
        ValidatedMindmapRequest validatedRequest = requestValidator.validateAndProcess(
            request.getRepoUrl(),
            null,
            userId
        );

        // 2. FastAPI 통합 분석 요청 - analyzeRepository 메서드 사용
        String normalizedUrl = validatedRequest.getRepositoryInfo().getNormalizedUrl();
        String processedPrompt = validatedRequest.getProcessedPrompt();

        CompletableFuture.supplyAsync(() -> {
            // 1. 요청 검증 및 전처리
            log.info("FastAPI 분석 요청 시작 - URL: {}, 프롬프트 존재: {}",
                normalizedUrl, StringUtils.hasText(processedPrompt));

            // prompt가 null이면 기본 분석, 있으면 프롬프트 포함 분석
            return fastApiClient.analyzeResult(
                normalizedUrl,
                null,
                authHeader
            );
        }).thenApply(analysisResult -> {
            // 2. 분석 결과를 바탕으로 DB에 마인드맵 정보 저장 (트랜잭션)
            log.info("분석 완료, DB 저장 시작 - 사용자: {}", userId);
            return mindmapService.saveMindmapFromAnalysis(analysisResult, normalizedUrl, request.getTitle(), userId);
        }).whenComplete((mindmap, throwable) -> {
            // 3. 최종 결과에 따라 알림 전송
            if (throwable != null) {
                handleFailureAndNotify(throwable, userId, normalizedUrl);
            } else {
                handleSuccessAndNotify(mindmap, userId);
            }
        });
    }

    @Async("mindmapExecutor")
    public void refreshMindmap(Long mapId, String authHeader) {
        try {
            log.info("비동기 새로고침 시작 - 마인드맵 ID: {}", mapId);
            Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
                .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

            PromptHistory appliedPrompt = mindmap.getAppliedPromptHistory();

            String repoUrl = mindmap.getRepo().getGithubRepoUrl();
            String prompt = (appliedPrompt != null) ? appliedPrompt.getPrompt() : null;

            // FastAPI 분석 요청
            AnalysisResultDto analysisResult = fastApiClient.refreshMindmap(
                repoUrl,
                prompt,
                authHeader
            );

            // 분석 결과를 DB에 업데이트 (트랜잭션)
            mindmapService.updateMindmapFromAnalysis(mapId, authHeader, analysisResult);
            log.info("비동기 새로고침 성공 - 마인드맵 ID: {}", mapId);

        } catch (Exception e) {
            log.error("비동기 새로고침 실패 - 마인드맵 ID: {}, 원인: {}", mapId, e.getMessage(), e);
        }
    }

    /**
     * 마인드맵 삭제 후 비동기 후처리 (ArangoDB 데이터 삭제 요청)
     */
    @Async("mindmapExecutor")
    public void cleanUpMindmapData(String repoUrl, String authorizationHeader) {
        try {
            fastApiClient.deleteMindmapData(repoUrl, authorizationHeader);
            log.info("ArangoDB 데이터 비동기 삭제 완료: {}", repoUrl);
        } catch (Exception e) {
            log.error("ArangoDB 데이터 비동기 삭제 실패 - 저장소: {}, 원인: {}", repoUrl, e.getMessage());
            // TODO: 실패 시 재시도 로직 또는 관리자 알림 등의 후속 처리 구현 가능
        }
    }

    private void handleSuccessAndNotify(Mindmap mindmap, Long userId) {
        try {
            User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

            String successMessage = String.format("마인드맵 '%s' 생성이 완료되었습니다.", mindmap.getTitle());
            notificationService.createAndSendNotification(
                NotificationCreateDto.actionable(
                    user,
                    NotificationType.SYSTEM_UPDATE,
                    successMessage,
                    mindmap.getId(),
                    null
                )
            );
            log.info("마인드맵 생성 성공 및 알림 전송 완료 - ID: {}, 사용자: {}", mindmap.getId(), userId);
        } catch (Exception e) {
            log.error("성공 알림 전송 실패 - 마인드맵 ID: {}, 사용자 ID: {}, 오류: {}",
                mindmap.getId(), userId, e.getMessage());
        }
    }

    private void handleFailureAndNotify(Throwable throwable, Long userId, String repoUrl) {
        final Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        log.error("마인드맵 생성 최종 실패 - 사용자: {}, 저장소: {}, 원인: {}", userId, repoUrl, cause.getMessage());

        try {
            userRepository.findByIdAndDeletedAtIsNull(userId).ifPresent(user -> {
                String errorMessage = "마인드맵 생성에 실패했습니다: " + getSimplifiedErrorMessage(cause);
                notificationService.createAndSendNotification(
                    NotificationCreateDto.simple(
                        user,
                        NotificationType.SYSTEM_UPDATE,
                        errorMessage
                    )
                );
            });
        } catch (Exception e) {
            log.error("실패 알림 전송 중 추가 오류 발생: {}", e.getMessage());
        }
    }

    private String getSimplifiedErrorMessage(Throwable throwable) {
        if (throwable instanceof GlobalException) {
            return ((GlobalException) throwable).getErrorCode().getMessage();
        }
        String message = throwable.getMessage();
        if (message == null) return "알 수 없는 오류";
        if (message.contains("timeout")) return "처리 시간이 초과되었습니다.";
        if (message.contains("404")) return "저장소를 찾을 수 없습니다.";
        if (message.contains("403")) return "저장소에 접근할 수 없습니다.";
        return "처리 중 오류가 발생했습니다.";
    }
}