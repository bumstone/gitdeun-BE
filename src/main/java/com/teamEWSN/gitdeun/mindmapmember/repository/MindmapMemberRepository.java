package com.teamEWSN.gitdeun.mindmapmember.repository;

import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;

@Repository
public interface MindmapMemberRepository extends JpaRepository<MindmapMember, Long>  {

    /* OWNER/EDITOR/VIEWER 여부 */
    boolean existsByMindmapIdAndUserId(Long mindmapId, Long userId);

    boolean existsByMindmapIdAndUserIdAndRole(Long mindmapId, Long userId, MindmapRole role);

    boolean existsByMindmapIdAndUserIdAndRoleIn(Long mindmapId, Long userId, Collection<MindmapRole> roles);

    // 권한 변경
    Optional<MindmapMember> findByIdAndMindmapId(Long memberId, Long mindmapId);

    // OWNER가 멤버 추방
    void deleteByIdAndMindmapId(Long memberId, Long mindmapId);
}
