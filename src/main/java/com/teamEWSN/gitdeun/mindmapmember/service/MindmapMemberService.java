package com.teamEWSN.gitdeun.mindmapmember.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MindmapMemberService {

    private final MindmapAuthService auth;
    private final MindmapMemberRepository memberRepository;

    // 멤버 권한 변경
    @Transactional
    public void changeRole(Long mapId, Long memberId,
                           MindmapRole newRole, Long requesterId) {

        // 호출자가 OWNER인지 확인
        if (!auth.isOwner(mapId, requesterId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 대상 멤버 조회 후 role 변경
        MindmapMember member = memberRepository.findByIdAndMindmapId(memberId, mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MEMBER_NOT_FOUND));
        member.updateRole(newRole);
    }

    // 멤버 추방
    @Transactional
    public void removeMember(Long mapId, Long memberId, Long requesterId) {
        if (!auth.isOwner(mapId, requesterId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }
        memberRepository.deleteByIdAndMindmapId(memberId, mapId);
    }
}
