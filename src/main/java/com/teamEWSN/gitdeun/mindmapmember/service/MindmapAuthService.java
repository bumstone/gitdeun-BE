package com.teamEWSN.gitdeun.mindmapmember.service;

import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MindmapAuthService {

    private final MindmapMemberRepository memberRepository;
    private final MindmapRepository mindmapRepository;

    /** OWNER 확인 - 삭제되지 않은 마인드맵만 */
    public boolean isOwner(Long mapId, Long userId) {
        return mindmapRepository.findByIdAndDeletedAtIsNull(mapId).isPresent() &&
            memberRepository.existsByMindmapIdAndUserIdAndRole(mapId, userId, MindmapRole.OWNER);
    }

    /** 수정 권한(OWNER, EDITOR) - 삭제되지 않은 마인드맵만 */
    public boolean hasEdit(Long mapId, Long userId) {
        return mindmapRepository.findByIdAndDeletedAtIsNull(mapId).isPresent() &&
            memberRepository.existsByMindmapIdAndUserIdAndRoleIn(
                mapId, userId, List.of(MindmapRole.OWNER, MindmapRole.EDITOR));
    }

    /** 열람 권한(모든 멤버) - 삭제되지 않은 마인드맵만 */
    public boolean hasView(Long mapId, Long userId) {
        return mindmapRepository.findByIdAndDeletedAtIsNull(mapId).isPresent() &&
            memberRepository.existsByMindmapIdAndUserId(mapId, userId);
    }
}
