package com.teamEWSN.gitdeun.invitation.entity;

import com.teamEWSN.gitdeun.common.util.CreatedEntity;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmapmember.entity.MindmapRole;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더만 사용하도록 강제
@Builder(toBuilder = true)
@Table(name = "invitation")
public class Invitation extends CreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    // 초대한 사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inviter_id", nullable = false)
    private User inviter;

    // 초대받은 사람 (이메일 초대의 경우)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    @Column(length = 36, nullable = false, unique = true)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MindmapRole role; // 초대 시 부여할 권한

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvitationStatus status;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt; // 초대 만료 시간


    public Invitation accept() {
        this.status = InvitationStatus.ACCEPTED;
        return this;
    }

    public void reject() {
        this.status = InvitationStatus.REJECTED;
    }
}