package com.teamEWSN.gitdeun.mindmapmember.repository;

import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MindmapMemberRepository extends JpaRepository<MindmapMember, Long>  {

    /* OWNER/EDITOR/VIEWER 여부 */
    // 삭제되지 않은 마인드맵의 멤버십만 확인
    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
        "FROM MindmapMember m WHERE m.mindmap.id = :mindmapId AND m.user.id = :userId " +
        "AND m.mindmap.deletedAt IS NULL")
    boolean existsByMindmapIdAndUserId(@Param("mindmapId") Long mindmapId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
        "FROM MindmapMember m WHERE m.mindmap.id = :mindmapId AND m.user.id = :userId " +
        "AND m.role = :role AND m.mindmap.deletedAt IS NULL")
    boolean existsByMindmapIdAndUserIdAndRole(@Param("mindmapId") Long mindmapId,
                                              @Param("userId") Long userId,
                                              @Param("role") MindmapRole role);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
        "FROM MindmapMember m WHERE m.mindmap.id = :mindmapId AND m.user.id = :userId " +
        "AND m.role IN :roles AND m.mindmap.deletedAt IS NULL")
    boolean existsByMindmapIdAndUserIdAndRoleIn(@Param("mindmapId") Long mindmapId,
                                                @Param("userId") Long userId,
                                                @Param("roles") Collection<MindmapRole> roles);

    // 삭제되지 않은 마인드맵의 멤버만 조회(권한 변경)
    @Query("SELECT m FROM MindmapMember m WHERE m.id = :memberId AND m.mindmap.id = :mindmapId " +
        "AND m.mindmap.deletedAt IS NULL")
    Optional<MindmapMember> findByIdAndMindmapId(@Param("memberId") Long memberId, @Param("mindmapId") Long mindmapId);

    // OWNER가 멤버 추방
    @Modifying
    @Query("DELETE FROM MindmapMember m WHERE m.id = :memberId AND m.mindmap.id = :mindmapId " +
        "AND m.mindmap.deletedAt IS NULL")
    void deleteByIdAndMindmapId(@Param("memberId") Long memberId, @Param("mindmapId") Long mindmapId);

    @Query("SELECT m FROM MindmapMember m WHERE m.mindmap.id = :mapId AND m.role = :role " +
        "AND m.mindmap.deletedAt IS NULL")
    Optional<MindmapMember> findByMindmapIdAndRole(@Param("mapId") Long mapId, @Param("role") MindmapRole role);

    // 마인드맵 ID와 사용자 ID로 멤버 조회
    Optional<MindmapMember> findByMindmapIdAndUserId(Long mindmapId, Long userId);

}
