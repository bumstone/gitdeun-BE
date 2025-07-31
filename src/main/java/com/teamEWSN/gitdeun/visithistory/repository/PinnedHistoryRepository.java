package com.teamEWSN.gitdeun.visithistory.repository;

import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.visithistory.entity.PinnedHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinnedHistoryRepository extends JpaRepository<PinnedHistory, Long> {

    boolean existsByUserIdAndVisitHistoryId(Long userId, Long historyId);

    Optional<PinnedHistory> findByUserIdAndVisitHistoryId(Long userId, Long historyId);

    // 사용자의 핀 고정 기록 최신순 조회
    List<PinnedHistory> findByUserOrderByCreatedAtDesc(User user);
}
