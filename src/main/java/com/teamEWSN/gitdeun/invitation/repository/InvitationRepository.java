package com.teamEWSN.gitdeun.invitation.repository;

import com.teamEWSN.gitdeun.invitation.entity.Invitation;
import com.teamEWSN.gitdeun.invitation.entity.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // 특정 마인드맵의 모든 초대 목록을 페이징하여 조회
    Page<Invitation> findByMindmapId(Long mindmapId, Pageable pageable);

    // 특정 마인드맵에 특정 유저가 이미 초대 대기중인지 확인
    boolean existsByMindmapIdAndInviteeIdAndStatusAndExpiresAtAfter(Long mindmapId, Long inviteeId, InvitationStatus status, LocalDateTime now);

    boolean existsByMindmapIdAndInviteeIdAndStatus(Long mindmapId, Long inviteeId, InvitationStatus status);

    // 사용자가 받은 모든 초대 목록 조회
    List<Invitation> findByInviteeIdAndStatus(Long inviteeId, InvitationStatus status);

    // 고유 토큰으로 초대 정보 조회
    Optional<Invitation> findByToken(String token);

}