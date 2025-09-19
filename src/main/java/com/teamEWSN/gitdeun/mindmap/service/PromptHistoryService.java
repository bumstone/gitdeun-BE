package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.AnalysisResultDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.MindmapPromptAnalysisDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptApplyRequestDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptHistoryResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import com.teamEWSN.gitdeun.mindmap.mapper.PromptHistoryMapper;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmap.repository.PromptHistoryRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromptHistoryService {

    private final PromptHistoryRepository promptHistoryRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapAuthService mindmapAuthService;
    private final FastApiClient fastApiClient;
    private final MindmapSseService mindmapSseService;
    private final PromptHistoryMapper promptHistoryMapper;

    /**
     * 프롬프트 분석 및 미리보기 생성
     */
    public PromptPreviewResponseDto createPromptPreview(Long mapId, Long userId, MindmapPromptAnalysisDto req, String authorizationHeader) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        String repoUrl = mindmap.getRepo().getGithubRepoUrl();

        try {
            AnalysisResultDto analysisResult = fastApiClient.refreshMindmap(repoUrl, req.getPrompt(), authorizationHeader);

            // FastAPI로부터 받은 analysisSummary 사용
            String summary = analysisResult.getTitle();

            // analysisSummary가 없거나 비어있는 경우 대체 로직 사용
            if (summary == null || summary.trim().isEmpty()) {
                summary = generateFallbackSummary(req.getPrompt());
            }

            PromptHistory history = PromptHistory.builder()
                .mindmap(mindmap)
                .prompt(req.getPrompt())
                .title(summary)
                .applied(false)
                .build();

            promptHistoryRepository.save(history);

            log.info("프롬프트 미리보기 생성 완료 - 마인드맵 ID: {}, 히스토리 ID: {}", mapId, history.getId());

            // 매퍼를 활용한 변환
            return promptHistoryMapper.toPreviewResponseDto(history);

        } catch (Exception e) {
            log.error("프롬프트 미리보기 생성 실패: {}", e.getMessage(), e);
            throw new RuntimeException("프롬프트 분석 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 프롬프트 히스토리 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<PromptHistoryResponseDto> getPromptHistories(Long mapId, Long userId, Pageable pageable) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        Page<PromptHistory> historiesPage = promptHistoryRepository.findByMindmapIdOrderByCreatedAtDesc(mapId, pageable);

        return historiesPage.map(promptHistoryMapper::toResponseDto);
    }

    /**
     * 특정 프롬프트 히스토리의 상세 미리보기 조회
     */
    @Transactional(readOnly = true)
    public PromptPreviewResponseDto getPromptHistoryPreview(Long mapId, Long historyId, Long userId) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        PromptHistory history = promptHistoryRepository.findByIdAndMindmapId(historyId, mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));

        // 매퍼 활용
        return promptHistoryMapper.toPreviewResponseDto(history);
    }

    /**
     * 현재 적용된 프롬프트 히스토리 조회
     */
    @Transactional(readOnly = true)
    public PromptHistoryResponseDto getAppliedPromptHistory(Long mapId, Long userId) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return promptHistoryRepository.findAppliedPromptByMindmapId(mapId)
            .map(promptHistoryMapper::toResponseDto)
            .orElse(null);
    }

    /**
     * 프롬프트 히스토리 적용
     */
    public void applyPromptHistory(Long mapId, Long userId, PromptApplyRequestDto req) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        PromptHistory historyToApply = promptHistoryRepository.findByIdAndMindmapId(req.getHistoryId(), mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));

        mindmap.applyPromptHistory(historyToApply);

        // 마인드맵 제목도 프롬프트 타이틀로 업데이트
        mindmap.updateTitle(historyToApply.getTitle());

        log.info("프롬프트 히스토리 적용 완료 - 마인드맵 ID: {}, 히스토리 ID: {}, 제목 변경: {}",
            mapId, req.getHistoryId(), historyToApply.getTitle());

        // 제목 변경 브로드캐스트 추가
        mindmapSseService.broadcastTitleChanged(mapId, historyToApply.getTitle());


        mindmapSseService.broadcastPromptApplied(mapId, historyToApply.getId());
    }

    /**
     * 프롬프트 히스토리 삭제
     */
    public void deletePromptHistory(Long mapId, Long historyId, Long userId) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        PromptHistory history = promptHistoryRepository.findByIdAndMindmapId(historyId, mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));

        if (history.getApplied()) {
            throw new GlobalException(ErrorCode.CANNOT_DELETE_APPLIED_PROMPT);
        }

        promptHistoryRepository.delete(history);
        log.info("프롬프트 히스토리 삭제 완료 - 히스토리 ID: {}", historyId);
    }

    /**
     * 마인드맵 생성 시 초기 프롬프트 히스토리 생성
     */
    public void createInitialPromptHistory(Mindmap mindmap, String prompt, String promptTitle) {
        if (prompt != null && !prompt.trim().isEmpty()) {
            PromptHistory history = PromptHistory.builder()
                .mindmap(mindmap)
                .prompt(prompt)
                .title(promptTitle)
                .applied(true)
                .build();

            promptHistoryRepository.save(history);
            log.info("초기 프롬프트 히스토리 생성 완료 - 마인드맵 ID: {}", mindmap.getId());
        }
    }

    /**
     * 프롬프트 결과 대체 요약 생성
     */
    private String generateFallbackSummary(String prompt) {
        if (prompt == null) {
            return "기본 분석";
        }

        if (prompt.length() > 24) {
            return prompt.substring(0, 24) + "...";
        }
        return prompt;
    }
}
