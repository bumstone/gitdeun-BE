package com.teamEWSN.gitdeun.common.jwt;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void saveRefreshToken(String refreshToken, String email, long refreshTokenExpired) {
        RefreshToken token = RefreshToken.builder()
            .refreshToken(refreshToken)
            .email(email)
            .issuedAt(System.currentTimeMillis())
            .ttl(refreshTokenExpired) // @TimeToLive에 사용될 만료 시간
            .build();

        refreshTokenRepository.save(token);
    }


    public Optional<RefreshToken> getRefreshToken(String refreshToken) {
        return refreshTokenRepository.findById(refreshToken);
    }


    public void deleteRefreshToken(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }

}
