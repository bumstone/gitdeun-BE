package com.teamEWSN.gitdeun.visithistory.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
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
@Transactional
@RequiredArgsConstructor
public class PinnedHistoryService {

    private final PinnedHistoryRepository pinnedHistoryRepository;
    private final UserRepository userRepository;
    private final VisitHistoryRepository visitHistoryRepository;

    public void fixPinned(Long historyId, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(USER_NOT_FOUND_FIX_PIN));

        VisitHistory visitHistory = visitHistoryRepository.findById(historyId)
            .orElseThrow(() -> new GlobalException(HISTORY_NOT_FOUND));

        // 이미 핀 고정이 있는지 확인
        if (pinnedHistoryRepository.existsByUserIdAndVisitHistoryId(userId, historyId)) {
            throw new GlobalException(PINNEDHISTORY_ALREADY_EXISTS);
        }

        PinnedHistory pin = PinnedHistory.builder()
            .user(user)
            .visitHistory(visitHistory)
            .build();

        pinnedHistoryRepository.save(pin);

    }

    @Transactional
    public void removePinned(Long historyId, Long userId) {
        PinnedHistory pin = pinnedHistoryRepository.findByUserIdAndVisitHistoryId(userId, historyId)
            .orElseThrow(() -> new GlobalException(PINNEDHISTORY_NOT_FOUND));

        pinnedHistoryRepository.delete(pin);

    }
}
