package com.teamEWSN.gitdeun.Recruitment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@Table(name = "recruitment_image", indexes = {
    @Index(name = "idx_recruitment_image_recruitment_id_deleted_at", columnList = "recruitment_id, deleted_at")
})
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruitment_id")
    private Recruitment recruitment;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Soft delete 처리 메소드
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
