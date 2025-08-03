package com.teamEWSN.gitdeun.common.oauth.entity;

import com.teamEWSN.gitdeun.common.converter.CryptoConverter;
import com.teamEWSN.gitdeun.common.util.AuditedEntity;
import com.teamEWSN.gitdeun.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "social_connection")
public class SocialConnection extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private OauthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "access_token", length = 1024, nullable = false)
    private String accessToken;

    @Convert(converter = CryptoConverter.class)
    @Column(name = "refresh_token", length = 1024)
    private String refreshToken;


    @Builder
    public SocialConnection(User user, OauthProvider provider, String providerId, String accessToken, String refreshToken) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public void updateTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken != null ? refreshToken : this.refreshToken;
    }
}
