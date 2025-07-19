package com.teamEWSN.gitdeun.common.jwt;

import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.user.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Getter
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private BlacklistService blackListService;

    @Autowired
    private JwtTokenParser jwtTokenParser;

    @Value("${jwt.access-expired}")
    private Long accessTokenExpired;

    @Value("${jwt.refresh-expired}")
    private Long refreshTokenExpired;


    public JwtTokenProvider(@Value("${jwt.secret-key}") String secretKey) {
        byte[] keyBytes = secretKey.getBytes();
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // 토큰 생성 - 유저 정보 이용
    public JwtToken generateToken(Authentication authentication) {

        long now = (new Date()).getTime();
        Date accessTokenExpiration = new Date(now + accessTokenExpired * 1000);

        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) authentication.getPrincipal();

        Long userId = ((CustomUserDetails) userPrincipal).getId();

        String jti = UUID.randomUUID().toString();
        // Access Token 생성
        String accessToken = Jwts.builder()
            .subject(String.valueOf(userId)) // Subject를 불변값인 userId로 설정
            .issuedAt(new Date()) // 발행 시간
            .id(jti)  // blacklist 관리를 위한 jwt token id
            .claim("email", userPrincipal.getEmail()) // 이메일
            .claim("nickname", userPrincipal.getNickname()) // 닉네임
            .claim("role", userPrincipal.getRole()) // 사용자 역할(Role)
            .claim("name",userPrincipal.getName())
            .claim("profileImage", userPrincipal.getProfileImage()) // 프로필 이미지 추가
            .expiration(accessTokenExpiration) // 만료 시간
            .signWith(secretKey) // 서명
            .compact();

        // Refresh Token 생성 (임의의 값 생성)
        String refreshToken = UUID.randomUUID().toString();

        // Redis에 Refresh Token 정보 저장
        refreshTokenService.saveRefreshToken( refreshToken, userPrincipal.getEmail(), refreshTokenExpired);


        // JWT Token 객체 반환
        return JwtToken.builder()
            .grantType("Bearer")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();

    }


    // 토큰에서 유저 정보 추출
    public Authentication getAuthentication(String accessToken) {
        // 토큰에서 Claims 추출
        Claims claims = jwtTokenParser.parseClaims(accessToken);

        // 권한 정보 확인
        if (claims.get("role") == null) {
            throw new GlobalException(ErrorCode.ROLE_NOT_FOUND);
        }

        // 클레임에서 모든 사용자 정보 추출
        Long id = Long.parseLong(claims.getSubject());
        String email = claims.get("email", String.class);
        String nickname = claims.get("nickname", String.class);
        String name = claims.get("name", String.class);
        String profileImage = claims.get("profileImage", String.class);
        Role role = Role.valueOf(claims.get("role", String.class));

        CustomUserDetails userDetails = new CustomUserDetails(id, email, nickname, profileImage, role, name);

        Collection<? extends GrantedAuthority> authorities =
            Collections.singletonList(role::name);

        // Authentication 객체 반환
        return new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

    }

    // 토큰 정보 검증
    public boolean validateToken(String token) {
        log.debug("validateToken start");
        try {
            Jws<Claims> claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);

            String jti = claims.getPayload().getId(); // JTI 추출
            return !blackListService.isTokenBlacklisted(jti);

        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT Token", e);
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT Token", e);
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT Token", e);
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty.", e);
        }
        return false;
    }

}