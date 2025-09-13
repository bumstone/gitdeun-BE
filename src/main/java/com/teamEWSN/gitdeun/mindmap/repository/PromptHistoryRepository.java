package com.teamEWSN.gitdeun.mindmap.repository;

import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    /**
     * 특정 마인드맵의 프롬프트 히스토리를 최신순으로 조회
     */
    @Query("SELECT p FROM PromptHistory p WHERE p.mindmap.id = :mindmapId ORDER BY p.createdAt DESC")
    List<PromptHistory> findByMindmapIdOrderByCreatedAtDesc(@Param("mindmapId") Long mindmapId);

    /**
     * 현재 적용된 프롬프트 히스토리 조회
     */
    @Query("SELECT p FROM PromptHistory p WHERE p.mindmap.id = :mindmapId AND p.applied = true")
    Optional<PromptHistory> findAppliedPromptByMindmapId(@Param("mindmapId") Long mindmapId);

    /**
     * 모든 프롬프트 히스토리의 적용 상태 해제
     */
    @Modifying
    @Query("UPDATE PromptHistory p SET p.applied = false WHERE p.mindmap.id = :mindmapId")
    void unapplyAllPrompts(@Param("mindmapId") Long mindmapId);

    /**
     * 특정 히스토리의 적용 상태 확인
     */
    boolean existsByIdAndAppliedTrue(Long historyId);

    /**
     * 마인드맵과 히스토리 ID로 조회 (권한 검증)
     */
    @Query("SELECT p FROM PromptHistory p WHERE p.id = :historyId AND p.mindmap.id = :mindmapId")
    Optional<PromptHistory> findByIdAndMindmapId(@Param("historyId") Long historyId, @Param("mindmapId") Long mindmapId);
}
