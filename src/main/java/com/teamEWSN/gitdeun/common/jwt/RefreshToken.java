package com.teamEWSN.gitdeun.common.jwt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
@RedisHash("refreshToken")
public class RefreshToken {

    @Id
    private String refreshToken;

    @Indexed
    private Long userId;
    private Long issuedAt;

    // Time to live (TTL) 설정, Redis에 만료 시간을 설정
    @TimeToLive
    private Long ttl;
}