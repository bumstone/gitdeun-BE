package com.teamEWSN.gitdeun.common.oauth.repository;

import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialConnectionRepository extends JpaRepository<SocialConnection, Long> {

    /**
     * 소셜 플랫폼과 해당 플랫폼에서의 고유 ID로 소셜 연동 정보를 조회
     * @param provider 소셜 플랫폼 (GOOGLE, GITHUB 등)
     * @param providerId 해당 소셜 플랫폼에서의 사용자 고유 ID
     */
    Optional<SocialConnection> findByProviderAndProviderId(OauthProvider provider, String providerId);

    // 소셜 로그인한 사용자의 소셜 연동 정보 조회
    Optional<SocialConnection> findByUserIdAndProvider(Long userId, OauthProvider provider);
}
