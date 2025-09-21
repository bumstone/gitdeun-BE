package com.teamEWSN.gitdeun.invitation.service;

import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.invitation.dto.InvitationActionResponseDto;
import com.teamEWSN.gitdeun.invitation.dto.InvitationResponseDto;
import com.teamEWSN.gitdeun.invitation.dto.InviteRequestDto;
import com.teamEWSN.gitdeun.invitation.dto.LinkResponseDto;
import com.teamEWSN.gitdeun.invitation.entity.Invitation;
import com.teamEWSN.gitdeun.invitation.entity.InvitationStatus;
import com.teamEWSN.gitdeun.invitation.mapper.InvitationMapper;
import com.teamEWSN.gitdeun.invitation.repository.InvitationRepository;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapMember;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.mindmapmember.repository.MindmapMemberRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.notification.service.NotificationService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final MindmapRepository mindmapRepository;
    private final MindmapMemberRepository mindmapMemberRepository;
    private final MindmapAuthService mindmapAuthService;
    private final NotificationService notificationService;
    private final InvitationMapper invitationMapper;

    // TODO: 배포 시 이메일 주소 변경
    private static final String INVITATION_BASE_URL = "http://localhost:8080/invitations/";
    // private static final String INVITATION_BASE_URL = "https://gitdeun.site/invitations/";

    // 초대 전송(이메일 + 알림)
    @Transactional
    public void inviteUserByEmail(Long mapId, InviteRequestDto requestDto, Long inviterId) {
        // 마인드맵 소유자만 초대 가능
        if (!mindmapAuthService.isOwner(mapId, inviterId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        User invitee = userRepository.findByEmailAndDeletedAtIsNull(requestDto.getEmail())
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_EMAIL));

        // 본인 초대 불가
        if (inviterId.equals(invitee.getId())) {
            throw new GlobalException(ErrorCode.CANNOT_INVITE_SELF);
        }

        // 기존 멤버 여부 확인
        if (mindmapMemberRepository.existsByMindmapIdAndUserId(mapId, invitee.getId())) {
            throw new GlobalException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        // 이미 초대 거절한 사용자 확인
        if (invitationRepository.existsByMindmapIdAndInviteeIdAndStatus(mapId, invitee.getId(), InvitationStatus.REJECTED)) {
            throw new GlobalException(ErrorCode.INVITATION_REJECTED_USER);
        }

        // 이미 초대했는지 확인 (만료 체크)
        if (invitationRepository.existsByMindmapIdAndInviteeIdAndStatusAndExpiresAtAfter(mapId, invitee.getId(), InvitationStatus.PENDING, LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_ALREADY_EXISTS);
        }

        // 삭제된 마인드맵 제외
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Invitation invitation = Invitation.builder()
            .mindmap(mindmap)
            .inviter(inviter)
            .invitee(invitee)
            .token(UUID.randomUUID().toString())
            .role(requestDto.getRole())
            .status(InvitationStatus.PENDING)
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
        invitationRepository.save(invitation);

        // 알림 전송 + 이메일 전송
        notificationService.notifyInvitation(invitation);
    }

    // 초대한 목록 조회(member)
    @Transactional(readOnly = true)
    public Page<InvitationResponseDto> getInvitationsByMindmap(Long mapId, Long userId, Pageable pageable) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Page<Invitation> invitations = invitationRepository.findByMindmapId(mapId, pageable);

        return invitations.map(invitationMapper::toResponseDto);
    }

    // 초대 수락 목록 조회
    @Transactional(readOnly = true)
    public List<InvitationResponseDto> getAcceptedInvitationsByMindmap(Long mapId, Long userId) {
        // 권한 검증 - 최소 조회 권한이 있어야 함
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // ACCEPTED 상태의 초대 목록 조회
        List<Invitation> acceptedInvitations = invitationRepository.findByMindmapIdAndStatus(
            mapId, InvitationStatus.ACCEPTED);

        // 엔티티를 DTO로 변환하여 반환
        return acceptedInvitations.stream()
            .map(invitationMapper::toResponseDto)
            .collect(Collectors.toList());
    }

    // 초대 보류 목록 조회
    @Transactional(readOnly = true)
    public List<InvitationResponseDto> getPendingInvitationsByMindmap(Long mapId, Long userId) {
        // 권한 검증 - 최소 조회 권한이 있어야 함
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        List<Invitation> pendingInvitations = invitationRepository.findByMindmapIdAndStatus(
            mapId, InvitationStatus.PENDING);

        // 엔티티를 DTO로 변환하여 반환
        return pendingInvitations.stream()
            .map(invitationMapper::toResponseDto)
            .collect(Collectors.toList());
    }

    // 초대 수락
    @Transactional
    public Mindmap acceptInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVITATION_NOT_FOUND));

        // 초대시간 만료
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_EXPIRED);
        }

        // 초대된 마인드맵이 삭제되었는지 확인
        if (invitation.getMindmap().isDeleted()) {
            throw new GlobalException(ErrorCode.MINDMAP_NOT_FOUND);
        }

        // 이미 처리된 초대 확인
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new GlobalException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        // 초대 중복 여부
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }


        Invitation newInvitation = invitation.accept();
        MindmapMember newMember = MindmapMember.of(newInvitation.getMindmap(), newInvitation.getInvitee(), newInvitation.getRole());
        mindmapMemberRepository.save(newMember);

        notificationService.notifyAcceptance(invitation);

        return newInvitation.getMindmap();
    }

    // 초대 거절
    @Transactional
    public void rejectInvitation(Long invitationId, Long userId) {
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVITATION_NOT_FOUND));

        // 초대 시간 만료
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_EXPIRED);
        }

        // 이미 처리된 초대 확인
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new GlobalException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        // 초대 중복 여부
        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        invitation.reject();
    }

    // 초대 링크 생성(owner)
    @Transactional
    public LinkResponseDto createInvitationLink(Long mapId, Long inviterId) {
        if (!mindmapAuthService.isOwner(mapId, inviterId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 삭제된 마인드맵에는 초대할 수 없음
        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));
        User inviter = userRepository.findById(inviterId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Invitation invitation = Invitation.builder()
            .mindmap(mindmap)
            .inviter(inviter)
            .invitee(null) // 링크 초대는 처음엔 초대받는 사람이 없음
            .token(UUID.randomUUID().toString())
            .role(MindmapRole.VIEWER) // 링크 초대는 기본적으로 가장 낮은 권한 부여
            .status(InvitationStatus.PENDING) // 링크 자체는 PENDING 상태
            .expiresAt(LocalDateTime.now().plusDays(1))
            .build();
        invitationRepository.save(invitation);

        return new LinkResponseDto(INVITATION_BASE_URL + invitation.getToken());
    }

    // 초대 링크 접근
    @Transactional
    public void acceptInvitationByLink(String token, Long userId) {
        Invitation invitation = invitationRepository.findByToken(token)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVITATION_NOT_FOUND));

        Long mapId = invitation.getMindmap().getId();

        // 만료된 초대 여부 확인
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_EXPIRED);
        }

        // 기존 멤버 여부 확인
        if (mindmapMemberRepository.existsByMindmapIdAndUserId(mapId, userId)) {
            throw new GlobalException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        // 이미 초대 거절한 사용자 확인
        if (invitationRepository.existsByMindmapIdAndInviteeIdAndStatus(mapId, userId, InvitationStatus.REJECTED)) {
            throw new GlobalException(ErrorCode.INVITATION_REJECTED_USER);
        }

        // 초대된 마인드맵이 삭제되었는지 확인
        if (invitation.getMindmap().isDeleted()) {
            throw new GlobalException(ErrorCode.MINDMAP_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Invitation updatedInvitation = invitation.toBuilder()
            .invitee(user)
            .status(InvitationStatus.PENDING)
            .build();
        invitationRepository.save(updatedInvitation);

        notificationService.notifyLinkApprovalRequest(invitation);
    }

    // 초대 링크 수락(owner)
    @Transactional
    public InvitationActionResponseDto approveLinkInvitation(Long invitationId, Long ownerId) {
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVITATION_NOT_FOUND));

        // owner 확인
        if (!mindmapAuthService.isOwner(invitation.getMindmap().getId(), ownerId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 이미 처리된 초대 확인
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new GlobalException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        // 만료 시간 확인
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_EXPIRED);
        }

        // 입장 사용자 확인
        if (invitation.getInvitee() == null) {
            throw new GlobalException(ErrorCode.INVITATION_NOT_FOUND);
        }

        Invitation newInvitation = invitation.accept();
        MindmapMember newMember = MindmapMember.of(newInvitation.getMindmap(), newInvitation.getInvitee(), newInvitation.getRole());
        mindmapMemberRepository.save(newMember);

        // 참여 요청자에게 승인 알림 전송
        notificationService.notifyLinkApproval(newInvitation);

        return new InvitationActionResponseDto("초대 요청이 승인되었습니다.");
    }

    // Owner의 링크 초대 요청 거절
    @Transactional
    public InvitationActionResponseDto rejectLinkApproval(Long invitationId, Long ownerId) {
        Invitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new GlobalException(ErrorCode.INVITATION_NOT_FOUND));

        // owner 확인
        if (!mindmapAuthService.isOwner(invitation.getMindmap().getId(), ownerId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        // 이미 처리된 초대 확인
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new GlobalException(ErrorCode.INVITATION_ALREADY_PROCESSED);
        }

        // 만료 시간 확인
        if (invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new GlobalException(ErrorCode.INVITATION_EXPIRED);
        }

        // 입장 사용자 확인
        if (invitation.getInvitee() == null) {
            throw new GlobalException(ErrorCode.INVITATION_NOT_FOUND);
        }

        invitation.reject();

        // 참여 요청자에게 거절 알림 전송
        notificationService.notifyLinkRejection(invitation);

        return new InvitationActionResponseDto("초대 요청이 거부되었습니다.");
    }
}