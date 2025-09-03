package com.teamEWSN.gitdeun.Application.entity;

import com.teamEWSN.gitdeun.Recruitment.entity.Recruitment;
import com.teamEWSN.gitdeun.Recruitment.entity.RecruitmentField;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "application",
    uniqueConstraints = {
        // 동일 사용자·공고에 대해 "활성" 신청 중복 방지용 (WITHDRAWN 제외)
        @UniqueConstraint(name = "uk_active_application",
            columnNames = {"recruitment_id", "applicant_id", "active"})
    },
    indexes = {
        @Index(name = "idx_application_recruitment", columnList = "recruitment_id"),
        @Index(name = "idx_application_applicant", columnList = "applicant_id"),
        @Index(name = "idx_application_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 모집공고 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recruitment_id", nullable = false)
    private Recruitment recruitment;

    /** 신청 사용자 (User.id) */
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    /** 지원 분야 (작성자의 모집 분야 중에서 선택) */
    @Enumerated(EnumType.STRING)
    @Column(name = "applied_field", nullable = false, length = 32)
    private RecruitmentField appliedField;

    /** 지원 메세지 */
    @Column(name = "message", length = 1000)
    private String message;

    /** 신청 거절 사유(거절 시 선택) */
    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    /** 신청 상태 (검토 중 / 수락 / 거절 / 철회) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ApplicationStatus status;

    /** 활성 신청 여부 (WITHDRAWN 면 false) */
    @Column(nullable = false)
    private boolean active;


    // 상태 전이
    public void accept() { this.status = ApplicationStatus.ACCEPTED; }
    public void reject(String reason) { this.status = ApplicationStatus.REJECTED; this.rejectReason = reason; }
    public void withdraw() { this.status = ApplicationStatus.WITHDRAWN; this.active = false; }
}
