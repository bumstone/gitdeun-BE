package com.teamEWSN.gitdeun.mindmap.repository;

import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.entity.MindmapType;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MindmapRepository extends JpaRepository<Mindmap, Integer> {

    // 사용자가 생성한 확인용 마인드맵 중 가장 최근에 생성된 것(repo 무관)
    @Query("SELECT m FROM Mindmap m " +
        "WHERE m.user = :user AND m.type = 'CHECK' " +
        "ORDER BY m.createdAt DESC LIMIT 1")
    Optional<Mindmap> findTopByUserAndTypeOrderByCreatedAtDesc(
        @Param("user") User user
    );
}