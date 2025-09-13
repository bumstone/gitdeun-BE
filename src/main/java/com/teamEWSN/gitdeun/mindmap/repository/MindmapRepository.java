package com.teamEWSN.gitdeun.mindmap.repository;

import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MindmapRepository extends JpaRepository<Mindmap, Long> {

    /**
     * 사용자의 삭제되지 않은 마인드맵 개수 조회 (제목 자동 생성용)
     */
    @Query("SELECT COUNT(m) FROM Mindmap m WHERE m.user = :user AND m.deletedAt IS NULL")
    long countByUserAndDeletedAtIsNull(@Param("user") User user);

    /**
     * 삭제되지 않은 마인드맵 조회
     */
    @Query("SELECT m FROM Mindmap m WHERE m.id = :id AND m.deletedAt IS NULL")
    Optional<Mindmap> findByIdAndNotDeleted(@Param("id") Long id);

    /**
     * 삭제되지 않은 마인드맵 목록 조회
     */
    @Query("SELECT m FROM Mindmap m WHERE m.user = :user AND m.deletedAt IS NULL ORDER BY m.updatedAt DESC")
    List<Mindmap> findByUserAndNotDeleted(@Param("user") User user);

}