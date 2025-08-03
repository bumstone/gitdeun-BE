package com.teamEWSN.gitdeun.user.entity;

import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, length = 100, unique = true)
    private String nickname;

    @Column(nullable = false, length = 256)
    private String email;

    @Column(name="profile_image", length = 512)
    private String profileImage;  // image url

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SocialConnection> socialConnections = new ArrayList<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    @Builder
    public User(String name, String nickname, String email, String profileImage, Role role) {
        this.name = name;
        this.nickname = nickname;
        this.email = email;
        this.profileImage = profileImage;
        this.role = role;
    }

    public User updateProfile(String name, String profileImage) {
        this.name = name;
        this.profileImage = profileImage;
        return this; // 메소드 체이닝을 위해 this 반환
    }

    // 회원 탈퇴 처리
    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now();
    }

    // 회원 닉네임 변경
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
    }


}