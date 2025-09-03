package com.teamEWSN.gitdeun.Recruitment.entity;

import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkillEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "recruitment_required_skill",
    uniqueConstraints = @UniqueConstraint(name = "uk_recruit_skill", columnNames = {"recruitment_id","skill"}),
    indexes = {
        @Index(name = "idx_recruit_skill_recruitment", columnList = "recruitment_id"),
        @Index(name = "idx_recruit_skill_category", columnList = "category"),
        @Index(name = "idx_rrs_cat_weight_skill", columnList = "category, weight, skill")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecruitmentRequiredSkill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private DeveloperSkillEnum skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SkillCategory category; // 추천은 LANGUAGE만 사용

    @Column(nullable = false)
    @Builder.Default
    private double weight = 1.0;
}
