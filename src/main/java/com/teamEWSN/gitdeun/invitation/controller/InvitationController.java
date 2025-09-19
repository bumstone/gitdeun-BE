package com.teamEWSN.gitdeun.invitation.controller;

import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.invitation.dto.InvitationActionResponseDto;
import com.teamEWSN.gitdeun.invitation.dto.InvitationResponseDto;
import com.teamEWSN.gitdeun.invitation.dto.InviteRequestDto;
import com.teamEWSN.gitdeun.invitation.dto.LinkResponseDto;
import com.teamEWSN.gitdeun.invitation.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    // 이메일로 멤버 초대
    @PostMapping("/mindmaps/{mapId}")
    public ResponseEntity<Void> inviteByEmail(
        @PathVariable Long mapId,
        @Valid @RequestBody InviteRequestDto requestDto,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        invitationService.inviteUserByEmail(mapId, requestDto, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 특정 마인드맵의 전체 초대 목록 조회 (페이지네이션 적용)
    @GetMapping("/mindmaps/{mapId}")
    public ResponseEntity<Page<InvitationResponseDto>> getInvitations(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(invitationService.getInvitationsByMindmap(mapId, userDetails.getId(), pageable));
    }

    // ACCEPTED 상태의 초대 목록 조회
    @GetMapping("/mindmaps/{mapId}/accepted")
    public ResponseEntity<List<InvitationResponseDto>> getAcceptedInvitations(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invitationService.getAcceptedInvitationsByMindmap(mapId, userDetails.getId()));
    }

    // PENDING 상태의 초대 목록 조회
    @GetMapping("/mindmaps/{mapId}/pending")
    public ResponseEntity<List<InvitationResponseDto>> getPendingInvitations(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invitationService.getPendingInvitationsByMindmap(mapId, userDetails.getId()));
    }

    // 초대 수락
    @PostMapping("/{invitationId}/accept")
    public ResponseEntity<Void> acceptInvitation(
        @PathVariable Long invitationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        invitationService.acceptInvitation(invitationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 초대 거절
    @PostMapping("/{invitationId}/reject")
    public ResponseEntity<Void> rejectInvitation(
        @PathVariable Long invitationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        invitationService.rejectInvitation(invitationId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }


    // 초대 링크 생성
    @PostMapping("/mindmaps/{mapId}/link")
    public ResponseEntity<LinkResponseDto> createInvitationLink(
        @PathVariable Long mapId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invitationService.createInvitationLink(mapId, userDetails.getId()));
    }

    // 초대 링크를 통해 수락 (로그인한 사용자가 링크 클릭 시)
    @PostMapping("/link/{token}/accept")
    public ResponseEntity<Void> acceptInvitationByLink(
        @PathVariable String token,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        invitationService.acceptInvitationByLink(token, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // Owner가 링크 초대 승인
    @PostMapping("/{invitationId}/approve")
    public ResponseEntity<InvitationActionResponseDto> approveLinkInvitation(
        @PathVariable Long invitationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invitationService.approveLinkInvitation(invitationId, userDetails.getId()));
    }
    // Owner가 링크 초대 거부
    @PostMapping("/{invitationId}/reject-link")
    public ResponseEntity<InvitationActionResponseDto> rejectLinkApproval(
        @PathVariable Long invitationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ResponseEntity.ok(invitationService.rejectLinkApproval(invitationId, userDetails.getId()));
    }
}