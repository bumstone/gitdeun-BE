package com.teamEWSN.gitdeun.mindmap.entity;

import com.teamEWSN.gitdeun.common.util.CreatedEntity;
import com.teamEWSN.gitdeun.repo.entity.Repo;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mindmap_member")
public class MindmapMember extends CreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mindmap_id", nullable = false)
    private Mindmap mindmap;

    // member
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private MindmapRole role;

    // 생성일이 멤버 수락일

    public static MindmapMember of(Mindmap mindmap, User user, MindmapRole role) {
        MindmapMember member = new MindmapMember();
        member.mindmap = mindmap;
        member.user = user;
        member.role = role;   // OWNER‧EDITOR‧VIEWER 등
        return member;
    }
}
