package com.teamEWSN.gitdeun.visithistory.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.service.MindmapService;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.service.UserService;
import com.teamEWSN.gitdeun.visithistory.dto.VisitHistoryResponseDto;
import com.teamEWSN.gitdeun.visithistory.entity.PinnedHistory;
import com.teamEWSN.gitdeun.visithistory.entity.VisitHistory;
import com.teamEWSN.gitdeun.visithistory.mapper.VisitHistoryMapper;
import com.teamEWSN.gitdeun.visithistory.repository.PinnedHistoryRepository;
import com.teamEWSN.gitdeun.visithistory.repository.VisitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VisitHistoryService {

    private final UserService userService;
    private final VisitHistoryRepository visitHistoryRepository;
    private final PinnedHistoryRepository pinnedHistoryRepository;
    private final VisitHistoryMapper visitHistoryMapper;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final MindmapService mindmapService;
    private final MindmapAuthService mindmapAuthService;

    // 마인드맵 생성 시 호출되어 방문 기록을 생성
    @Transactional
    public void createVisitHistory(User user, Mindmap mindmap) {
        // 삭제된 마인드맵에는 방문 기록을 생성하지 않음
        if (mindmap.isDeleted()) {
            return;
        }

        VisitHistory visitHistory = VisitHistory.builder()
            .user(user)
            .mindmap(mindmap)
            .lastVisitedAt(LocalDateTime.now())
            .build();
        visitHistoryRepository.save(visitHistory);
    }

    //  핀 고정되지 않은 방문 기록 조회
    @Transactional(readOnly = true)
    public Page<VisitHistoryResponseDto> getVisitHistories(Long userId, Pageable pageable) {
        User user = userService.findById(userId);

        // 삭제되지 않은 마인드맵 필터링
        Page<VisitHistory> histories = visitHistoryRepository.findUnpinnedHistoriesByUserAndNotDeletedMindmap(user, pageable);
        return histories.map(visitHistoryMapper::toResponseDto);
    }

    // 핀 고정된 방문 기록 조회(8개 제한)
    @Transactional(readOnly = true)
    public List<VisitHistoryResponseDto> getPinnedHistories(Long userId) {
        User user = userService.findById(userId);

        List<PinnedHistory> pinnedHistories = pinnedHistoryRepository.findTop8ByUserAndNotDeletedMindmapOrderByCreatedAtDesc(user);

        return pinnedHistories.stream()
            .map(pinned -> visitHistoryMapper.toResponseDto(pinned.getVisitHistory()))
            .collect(Collectors.toList());
    }

    // 방문 기록 삭제
    @Transactional
    public void deleteVisitHistory(Long visitHistoryId, Long userId) {
        VisitHistory visitHistory = visitHistoryRepository.findById(visitHistoryId)
            .orElseThrow(() -> new GlobalException(ErrorCode.HISTORY_NOT_FOUND));

        if (!visitHistory.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = visitHistory.getMindmap();
        Long mindmapId = mindmap.getId();

        boolean isOwner = mindmapAuthService.isOwner(mindmapId, userId);

        if (isOwner) {
            // 소유자인 경우, 마인드맵 전체를 삭제
            mindmapService.deleteMindmap(mindmapId, userId);
        } else {
            // 소유자가 아닌 경우, 마인드맵의 멤버 탈퇴
            mindmapMemberRepository.findByMindmapIdAndUserId(mindmapId, userId)
                .ifPresent(mindmapMemberRepository::delete);
        }

        // 마지막으로 방문 기록을 삭제
        visitHistoryRepository.delete(visitHistory);
    }


}