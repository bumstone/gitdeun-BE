package com.teamEWSN.gitdeun.mindmap.repository;

import com.teamEWSN.gitdeun.mindmap.entity.PromptHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromptHistoryRepository extends JpaRepository<PromptHistory, Long> {

    /**
     * 특정 마인드맵의 프롬프트 히스토리를 최신순으로 조회
     */
    Page<PromptHistory> findByMindmapIdOrderByCreatedAtDesc(Long mindmapId, Pageable pageable);

    /**
     * 현재 적용된 프롬프트 히스토리 조회
     */
    @Query("SELECT p FROM PromptHistory p WHERE p.mindmap.id = :mindmapId AND p.applied = true")
    Optional<PromptHistory> findAppliedPromptByMindmapId(@Param("mindmapId") Long mindmapId);

    /**
     * 마인드맵과 히스토리 ID로 조회 (권한 검증)
     */
    @Query("SELECT p FROM PromptHistory p WHERE p.id = :historyId AND p.mindmap.id = :mindmapId")
    Optional<PromptHistory> findByIdAndMindmapId(@Param("historyId") Long historyId, @Param("mindmapId") Long mindmapId);
}
