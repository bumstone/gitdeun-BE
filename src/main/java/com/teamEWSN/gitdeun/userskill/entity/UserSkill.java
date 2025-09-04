package com.teamEWSN.gitdeun.userskill.entity;

import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "user_skill", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_skill", columnList = "skill")
})
public class UserSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "INT UNSIGNED")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String skill;

    @Override
    public String toString() {
        return "{user_id: " + user.getId() + ", keyword: " +  skill + "}";
    }
}
