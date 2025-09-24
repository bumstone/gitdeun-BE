package com.teamEWSN.gitdeun.mindmap.util;

import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileContentCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FastApiClient fastApiClient;
    private final FileContentL1Cache l1Cache;

    @Component
    public static class FileContentL1Cache {
        @Cacheable(value = "FILE_CONTENT_L1", key = "#key",
            unless = "#result == null || #result.isEmpty()")
        public String getFromL1Cache(String key) {
            return null; // 미스면 null
        }

        @CachePut(value = "FILE_CONTENT_L1", key = "#key",
            condition = "#content != null && !#content.isBlank()")
        public String cacheToL1(String key, String content) {
            return content;
        }

        @CacheEvict(value = "FILE_CONTENT_L1", allEntries = true)
        public void evictL1Cache() {
        }
    }


    public String getFileContentWithCacheFromNode(String repoUrl, String nodeKey, String filePath, LocalDateTime lastCommit, String authHeader) {
        String cacheKey = "file-content:" + repoUrl + ":node:" + nodeKey + ":" + filePath + ":" + lastCommit.toString();

        // 1. L1 캐시 확인
        String content = l1Cache.getFromL1Cache(cacheKey);
        if (content != null) {
            log.debug("파일 내용 L1 캐시 히트 - key: {}", cacheKey);
            return content;
        }

        // 2. L2 캐시 확인
        try {
            content = (String) redisTemplate.opsForValue().get(cacheKey);
            if (content != null && !content.isBlank()) {
                log.debug("파일 내용 L2 캐시 히트 - key: {}", cacheKey);
                l1Cache.cacheToL1(cacheKey, content); // L1에 저장
                return content;
            } else if (content != null && content.isBlank()) {
                log.warn("L2 캐시에서 빈 문자열 발견 - key: {} (전파/재적재 하지 않음)", cacheKey);
                // 캐시에 남겨두지 말고 즉시 삭제
                try { redisTemplate.delete(cacheKey); } catch (Exception ignore) {}
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패, API 직접 호출 - key: {}", cacheKey, e);
        }

        // 3. FastAPI 실시간 조회 (새로 만든 메서드를 호출하도록 변경)
        content = fastApiClient.getCodeFromNode(nodeKey, filePath, authHeader);

        // 4. L1, L2 캐시에 저장
        if (content != null && !content.isBlank()) {
            l1Cache.cacheToL1(cacheKey, content);
            try {
                redisTemplate.opsForValue().set(cacheKey, content, Duration.ofHours(2));
            } catch (Exception e) {
                log.warn("Redis 저장 실패 - key: {}", cacheKey, e);
            }
        }
        return content;
    }


    // 캐시 무효화
    public void evictFileCacheForRepo(String repoUrl) {
        // L1 캐시는 전체 삭제
        l1Cache.evictL1Cache();
        deleteRedisKeysByPattern("file-content:" + repoUrl + ":*");
    }

    private void deleteRedisKeysByPattern(String pattern) {
        try {
            Set<String> keysToDelete = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> keys = new HashSet<>();
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(ScanOptions.scanOptions().match(pattern).count(100).build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return keys;
            });

            if (keysToDelete != null && !keysToDelete.isEmpty()) {
                redisTemplate.delete(keysToDelete);
                log.info("{}개의 L2 파일 캐시(Redis) 무효화 완료 - pattern: {}", keysToDelete.size(), pattern);
            }
        } catch (Exception e) {
            log.warn("Redis 파일 캐시 무효화 실패 - pattern: {}", pattern, e);
        }
    }
}