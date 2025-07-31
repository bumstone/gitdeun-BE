package com.teamEWSN.gitdeun.user.entity;

import com.teamEWSN.gitdeun.user.dto.UserSettingUpdateRequestDto;
import jakarta.persistence.Entity;
import lombok.*;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_settings")
public class UserSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // 화면 테마 (LIGHT, DARK)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ColumnDefault("'LIGHT'")
    private DisplayTheme theme = DisplayTheme.LIGHT;

    @Builder.Default
    @Column(name = "email_notification", nullable = false)
    @ColumnDefault("true")
    private boolean emailNotification = true;


    public enum DisplayTheme {
        LIGHT, DARK
    }

    @Builder
    private UserSetting(User user, DisplayTheme theme, boolean emailNotification) {
        this.user = user;
        this.theme = theme;
        this.emailNotification = emailNotification;
    }

    public static UserSetting createDefault(User user) {
        return UserSetting.builder()
            .user(user)
            .build();
    }

    public void update(UserSettingUpdateRequestDto dto) {
        this.theme = dto.getTheme();
        this.emailNotification = dto.getEmailNotification();
    }
}

