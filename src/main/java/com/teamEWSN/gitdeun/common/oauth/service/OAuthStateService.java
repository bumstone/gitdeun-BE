package com.teamEWSN.gitdeun.common.oauth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuthStateService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final Duration EXPIRATION = Duration.ofMinutes(3);

    public String createState(String purpose) {
        String state = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("oauth:state:" + state, purpose, EXPIRATION);
        return state;
    }

    public String consumeState(String state) {
        String key = "oauth:state:" + state;
        String purpose = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return purpose;
    }
}
