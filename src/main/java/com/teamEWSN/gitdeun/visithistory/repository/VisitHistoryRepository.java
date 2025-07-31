package com.teamEWSN.gitdeun.visithistory.repository;

import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.visithistory.entity.VisitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitHistoryRepository extends JpaRepository<VisitHistory, Long> {

    // 사용자의 핀 고정되지 않은 방문 기록을 최신순으로 조회
    @Query("SELECT v FROM VisitHistory v LEFT JOIN v.pinnedHistorys p " +
        "WHERE v.user = :user AND p IS NULL " +
        "ORDER BY v.lastVisitedAt DESC")
    List<VisitHistory> findUnpinnedHistoriesByUser(@Param("user") User user);
}