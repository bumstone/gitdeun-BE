package com.teamEWSN.gitdeun.visithistory.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import com.teamEWSN.gitdeun.visithistory.dto.PinnedHistoryUpdateDto;
import com.teamEWSN.gitdeun.visithistory.entity.PinnedHistory;
import com.teamEWSN.gitdeun.visithistory.entity.VisitHistory;
import com.teamEWSN.gitdeun.visithistory.repository.PinnedHistoryRepository;
import com.teamEWSN.gitdeun.visithistory.repository.VisitHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.teamEWSN.gitdeun.common.exception.ErrorCode.*;


@Slf4j
@Service
@RequiredArgsConstructor
public class PinnedHistoryService {

    private final PinnedHistoryRepository pinnedHistoryRepository;
    private final UserRepository userRepository;
    private final VisitHistoryRepository visitHistoryRepository;
    private final VisitHistoryBroadcastService visitHistoryBroadcastService;

    @Transactional
    public void fixPinned(Long historyId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(USER_NOT_FOUND_FIX_PIN));

        VisitHistory visitHistory = visitHistoryRepository.findById(historyId)
            .orElseThrow(() -> new GlobalException(HISTORY_NOT_FOUND));

        // 마인드맵이 삭제되었는지 확인
        if (visitHistory.getMindmap().isDeleted()) {
            throw new GlobalException(ErrorCode.MINDMAP_NOT_FOUND);
        }

        // 이미 핀 고정이 있는지 확인
        if (pinnedHistoryRepository.existsByUserIdAndVisitHistoryIdAndNotDeletedMindmap(userId, historyId)) {
            throw new GlobalException(PINNEDHISTORY_ALREADY_EXISTS);
        }

        // 현재 핀 개수 확인 (삭제되지 않은 마인드맵)
        long currentPinCount = pinnedHistoryRepository.countByUserAndNotDeletedMindmap(user);
        if (currentPinCount >= 8) {
            throw new GlobalException(PINNED_HISTORY_LIMIT_EXCEEDED);
        }

        PinnedHistory pin = PinnedHistory.builder()
            .user(user)
            .visitHistory(visitHistory)
            .build();

        pinnedHistoryRepository.save(pin);

        // 실시간 브로드캐스트 - 핀 고정 추가
        PinnedHistoryUpdateDto updateDto = PinnedHistoryUpdateDto.builder()
            .action("PIN_ADDED")
            .historyId(historyId)
            .mindmapId(visitHistory.getMindmap().getId())
            .mindmapTitle(visitHistory.getMindmap().getTitle())
            .currentPinCount(currentPinCount + 1)
            .maxPinCount(8)
            .build();

        visitHistoryBroadcastService.broadcastPinUpdate(userId, updateDto);

    }

    @Transactional
    public void removePinned(Long historyId, Long userId) {
        PinnedHistory pin = pinnedHistoryRepository.findByUserIdAndVisitHistoryId(userId, historyId)
            .orElseThrow(() -> new GlobalException(PINNEDHISTORY_NOT_FOUND));

        VisitHistory visitHistory = pin.getVisitHistory();
        pinnedHistoryRepository.delete(pin);

        // 현재 활성 핀 개수 계산 (삭제되지 않은 마인드맵만)
        long currentPinCount = pinnedHistoryRepository.countByUserAndNotDeletedMindmap(pin.getUser());

        // 실시간 브로드캐스트 - 핀 해제
        PinnedHistoryUpdateDto updateDto = PinnedHistoryUpdateDto.builder()
            .action("PIN_REMOVED")
            .historyId(historyId)
            .mindmapId(visitHistory.getMindmap().getId())
            .mindmapTitle(visitHistory.getMindmap().getTitle())
            .currentPinCount(currentPinCount)
            .maxPinCount(8)
            .build();

        visitHistoryBroadcastService.broadcastPinUpdate(userId, updateDto);

    }
}
