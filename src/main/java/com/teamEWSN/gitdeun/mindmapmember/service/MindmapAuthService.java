package com.teamEWSN.gitdeun.mindmapmember.service;

import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MindmapAuthService {

    private final MindmapMemberRepository memberRepo;

    /** OWNER 확인 */
    public boolean isOwner(Long mapId, Long userId) {
        return memberRepo.existsByMindmapIdAndUserIdAndRole(mapId, userId, MindmapRole.OWNER);
    }

    /** 수정 권한(OWNER, EDITOR) */
    public boolean hasEdit(Long mapId, Long userId) {
        return memberRepo.existsByMindmapIdAndUserIdAndRoleIn(
            mapId, userId, List.of(MindmapRole.OWNER, MindmapRole.EDITOR));
    }

    /** 열람 권한(모든 멤버) */
    public boolean hasView(Long mapId, Long userId) {
        return memberRepo.existsByMindmapIdAndUserId(mapId, userId);
    }
}
