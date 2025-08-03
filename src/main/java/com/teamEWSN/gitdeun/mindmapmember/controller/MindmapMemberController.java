package com.teamEWSN.gitdeun.mindmapmember.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmapmember.dto.RoleChangeRequestDto;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController("/api/mindmaps/{mapId}/members/{memberId}")
@RequiredArgsConstructor
public class MindmapMemberController {

    private final MindmapMemberService memberService;

    // 마인드맵 멤버 권한 변경
    @PatchMapping("/role")
    public ResponseEntity<Void> updateRole(
        @PathVariable Long mapId,
        @PathVariable Long memberId,
        @RequestBody RoleChangeRequestDto dto,
        @AuthenticationPrincipal CustomUserDetails user) {
        memberService.changeRole(mapId, memberId, dto.getRole(), user.getId());
        return ResponseEntity.ok().build();
    }

    // 마인드맵 멤버 추방
    @DeleteMapping
    public ResponseEntity<Void> kickMember(
        @PathVariable Long mapId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal CustomUserDetails user) {
        memberService.removeMember(mapId, memberId, user.getId());
        return ResponseEntity.ok().build();
    }
}

