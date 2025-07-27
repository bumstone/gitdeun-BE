package com.teamEWSN.gitdeun.common.oauth.service;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.oauth.entity.OauthProvider;
import com.teamEWSN.gitdeun.common.oauth.entity.SocialConnection;
import com.teamEWSN.gitdeun.common.oauth.record.GoogleTokenResponse;
import com.teamEWSN.gitdeun.common.oauth.repository.SocialConnectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.teamEWSN.gitdeun.common.exception.ErrorCode.*;


// 레포 및 마인드맵 호출 시 소셜로그인 토큰 갱신 호출
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SocialTokenRefreshService {

    private final SocialConnectionRepository socialConnectionRepository;
    private final GitHubApiHelper gitHubApiHelper;
    private final GoogleApiHelper googleApiHelper;


    // 기존 refreshToken 기반 갱신(주기적/자동 갱신)
    public void refreshSocialToken(Long userId, OauthProvider provider) {
        SocialConnection connection = socialConnectionRepository.findByUserIdAndProvider(userId, provider)
            .orElseThrow(() -> new GlobalException(SOCIAL_CONNECTION_NOT_FOUND));

        switch (provider) {
            case GOOGLE -> refreshGoogle(connection, Optional.empty(), Optional.empty());
            case GITHUB -> {
                log.warn("GitHub는 토큰 갱신을 지원하지 않습니다. 재인증이 필요합니다.");
                throw new GlobalException(ErrorCode.SOCIAL_TOKEN_REFRESH_NOT_SUPPORTED);
            }
        }

        // 갱신 후 저장 명시
        socialConnectionRepository.save(connection);
    }

    // oauth 새로운 토큰 제공 시 갱신(로그인 콜백)
    public void refreshSocialToken(SocialConnection conn,
                                   String latestAccess, String latestRefresh) {
        switch (conn.getProvider()) {
            case GOOGLE -> refreshGoogle(conn, Optional.ofNullable(latestAccess),
                Optional.ofNullable(latestRefresh));
            // GitHub은 refresh 불가
            case GITHUB -> conn.updateTokens(latestAccess, null);  // accessToken만 교체
        }

        socialConnectionRepository.save(conn);
    }


    private void refreshGoogle(SocialConnection conn,
                                 Optional<String> latestAccessOpt,
                                 Optional<String> latestRefreshOpt) {
        // 1. latestAccess가 주어지고 유효하면 교체
        if (latestAccessOpt.isPresent() && !googleApiHelper.isExpired(latestAccessOpt.get())) {
            String newRefresh = latestRefreshOpt.orElse(conn.getRefreshToken());
            conn.updateTokens(latestAccessOpt.get(), newRefresh);
            return;
        }

        // 2. refreshToken 기반 재발급 (latestRefresh가 있으면 그것 사용, 없으면 기존)
        String refreshToUse = latestRefreshOpt.orElse(conn.getRefreshToken());
        if (refreshToUse == null) {
            throw new GlobalException(INVALID_REFRESH_TOKEN);
        }

        GoogleTokenResponse res = googleApiHelper.refreshToken(refreshToUse);

        String newRefresh = (res.refreshToken() != null) ? res.refreshToken() : conn.getRefreshToken();
        conn.updateTokens(res.accessToken(), newRefresh);
    }

}
