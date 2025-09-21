package com.teamEWSN.gitdeun.visithistory.repository;

import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.visithistory.entity.PinnedHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PinnedHistoryRepository extends JpaRepository<PinnedHistory, Long> {

    /**
     * 삭제되지 않은 마인드맵의 핀 고정 기록만 조회 (최신순, 최대 8개)
     * - UI 표시용
     * - 소프트 삭제된 마인드맵은 제외
     */
    @Query("SELECT p FROM PinnedHistory p " +
        "JOIN p.visitHistory v " +
        "JOIN v.mindmap m " +
        "WHERE p.user = :user AND m.deletedAt IS NULL " +
        "ORDER BY p.createdAt DESC")
    List<PinnedHistory> findTop8ByUserAndNotDeletedMindmapOrderByCreatedAtDesc(@Param("user") User user);

    /**
     * 삭제되지 않은 마인드맵의 핀 고정 개수
     */
    @Query("SELECT COUNT(p) FROM PinnedHistory p " +
        "JOIN p.visitHistory v " +
        "JOIN v.mindmap m " +
        "WHERE p.user = :user AND m.deletedAt IS NULL")
    long countByUserAndNotDeletedMindmap(@Param("user") User user);

    /**
     * 삭제되지 않은 마인드맵의 특정 핀 고정 기록 존재 여부
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM PinnedHistory p " +
        "JOIN p.visitHistory v " +
        "JOIN v.mindmap m " +
        "WHERE p.user.id = :userId AND v.id = :historyId AND m.deletedAt IS NULL")
    boolean existsByUserIdAndVisitHistoryIdAndNotDeletedMindmap(@Param("userId") Long userId, @Param("historyId") Long historyId);

    @EntityGraph(attributePaths = {"visitHistory.mindmap", "visitHistory.mindmap.repo"})
    Optional<PinnedHistory> findByUserIdAndVisitHistoryId(Long userId, Long historyId);

}
