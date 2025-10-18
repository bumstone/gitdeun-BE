package com.teamEWSN.gitdeun.user.entity;

import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.userskill.entity.UserSkill;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends AuditedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(nullable = false, length = 50, unique = true)
    private String handle;               // @menton / URL 용 전역 유니크 핸들

    @Column(nullable = false, length = 256, unique = true)
    private String email;

    @Column(name="profile_image", length = 512)
    private String profileImage;  // image url

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // 소셜 연동
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialConnection> socialConnections = new ArrayList<>();

    // 사용자 기술
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserSkill> skills;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Builder
    public User(String name, String nickname, String handle, String email, String profileImage, Role role) {
        this.name = name;
        this.nickname = nickname;
        this.handle = handle;
        this.email = email;
        this.profileImage = profileImage;
        this.role = role;
    }

    public void updateProfile(String name, String nickname, String profileImage) {
        this.name = name;
        this.nickname = nickname;
        this.profileImage = profileImage;
    }

    // 회원 탈퇴 처리
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }


    /**
     * handle이 아직 비어 있는(=최초 보정이 필요한) 계정에서만 1회 설정을 허용합니다.
     * 이미 handle이 존재한다면 예외를 던져 서비스 내 불변성을 유지합니다.
     */
    public void setHandle(String newHandle) {
        if (this.handle != null && !this.handle.isBlank()) {
            throw new IllegalStateException("Handle is already set and cannot be changed.");
        }
        if (newHandle == null || newHandle.isBlank()) {
            throw new IllegalArgumentException("Handle cannot be null or blank.");
        }
        // 길이·문자 정책은 HandleGenerator에서 보장하지만, 방어적으로 한 번 더 체크해도 됩니다.
        if (newHandle.length() > 50) {
            throw new IllegalArgumentException("Handle exceeds maximum length of 50 characters.");
        }
        this.handle = newHandle;
    }

    /** 편의 메서드: handle 존재 여부 */
    public boolean hasHandle() {
        return this.handle != null && !this.handle.isBlank();
    }
}