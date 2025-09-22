package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.fastapi.FastApiClient.SuggestionAutoResponse;
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

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PromptHistoryService {

    private final PromptHistoryRepository promptHistoryRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapAuthService mindmapAuthService;
    private final MindmapSseService mindmapSseService;
    private final PromptHistoryMapper promptHistoryMapper;

    /**
     * FastAPI의 제안 분석 결과를 바탕으로 프롬프트 히스토리를 생성하고 DB에 저장
     */
    public PromptHistory createPromptHistoryFromSuggestion(Long mapId, String prompt, SuggestionAutoResponse suggestionResponse) {
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        // chosen_scopes 리스트의 첫 번째 항목을 summary로 사용
        String summary;
        List<String> scopes = suggestionResponse.getChosen_scopes();
        if (scopes != null && !scopes.isEmpty()) {
            summary = scopes.getFirst();
        } else {
            // fallback 로직: AI가 추천 스코프를 찾지 못한 경우
            summary = generateFallbackSummary(prompt);
        }

        PromptHistory history = PromptHistory.builder()
            .mindmap(mindmap)
            .prompt(prompt)
            .summary(summary)
            .applied(false)
            .build();

        PromptHistory savedHistory = promptHistoryRepository.save(history);
        log.info("프롬프트 히스토리 생성 완료 - 마인드맵 ID: {}, 히스토리 ID: {}", mapId, savedHistory.getId());

        // DTO로 변환하여 SSE로 브로드캐스트
        PromptPreviewResponseDto previewDto = promptHistoryMapper.toPreviewResponseDto(savedHistory);
        mindmapSseService.broadcastPromptReady(mapId, previewDto);

        return savedHistory;
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
    public void applyPromptHistory(Long mapId, Long historyId, Long userId) {
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        PromptHistory historyToApply = promptHistoryRepository.findByIdAndMindmapId(historyId, mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));

        mindmap.applyPromptHistory(historyToApply);

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
