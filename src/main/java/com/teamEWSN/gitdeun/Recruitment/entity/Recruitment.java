package com.teamEWSN.gitdeun.Recruitment.entity;

import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.userskill.entity.DeveloperSkill;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "recruitment",
    indexes = {
        @Index(name = "idx_recruitment_status", columnList = "status"),
        @Index(name = "idx_recruitment_deadline", columnList = "end_at"),
        @Index(name = "idx_recruitment_recruiter", columnList = "recruiter_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recruitment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 모집자
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "recruiter_id", nullable = false,
        foreignKey = @ForeignKey(name = "fk_recruitment_user")  // 외래키 값 명시
    )
    private User recruiter;

    // 모집 공고 제목 (입력 필요)
    @Column(nullable = false, length = 120)
    private String title;

    // 연락망: 이메일 주소 (입력 필요)
    @Column(length = 120)
    private String contactEmail;

    // 모집일 (입력 필요)
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    // 모집 마감일 (입력 필요)
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    // 모집 내용 (입력 필요)
    @Lob    // 해당 필드를 DB에서 대용량 저장하도록 매핑
    @Column(nullable = false)
    private String content;

    // 팀 규모(총인원) (입력 필요)
    @Column(name = "team_size_total", nullable = false)
    private Integer teamSizeTotal;

    // 남은 모집 인원(입력 필요) – 신청 시 1 감소, 철회/거절 시 1 증가(복원)
    @Column(name = "recruit_quota", nullable = false)
    private Integer recruitQuota;

    // 개발 분야 태그 (선택 필요) - BACKEND/FRONTEND/AI 등
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recruitment_field_tags", joinColumns = @JoinColumn(name = "recruitment_id"))
    @Column(name = "field", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<RecruitmentField> fieldTags = new HashSet<>();

    // 개발 언어 태그 (선택 필요) - 화면 필터/표시용
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "recruitment_language_tags", joinColumns = @JoinColumn(name = "recruitment_id"))
    @Column(name = "language", nullable = false, length = 64)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<DeveloperSkill> languageTags = new HashSet<>();

    // 모집 상태 (모집 예정 / 모집 중 / 모집 마감)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private RecruitmentStatus status;

    // 조회수
    @Column(name = "views", nullable = false)
    @Builder.Default
    private int views = 0;

    // 모집 공고 이미지 (선택)
    @Builder.Default
    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentImage> recruitmentImages = new ArrayList<>();

    /*// 지원 신청 리스트
    @OneToMany(mappedBy = "recruitment", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Application> applications = new ArrayList<>();*/

    // 추천 가중치용 요구 기술(언어/프레임워크/툴 등, 선택)
    @OneToMany(mappedBy = "recruitment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<RecruitmentRequiredSkill> requiredSkills = new HashSet<>();



    public void increaseView() { this.views++; }
}
