package com.teamEWSN.gitdeun.mindmap.util;

import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;          // ✅ spring-http 로 교체 (java.net.http 아님)
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate; // ✅ RestTemplate 주입
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileContentCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FastApiClient fastApiClient;
    private final FileContentL1Cache l1Cache;
    private final RestTemplate restTemplate;      // ✅ 빈으로 주입받음 (없으면 아래 참고)

    @Value("${fastapi.base-url:http://localhost:8000}") // ✅ 기본값 제공
    private String fastApiBaseUrl;

    // ===== L1 캐시(Spring Cache) 래퍼 =====
    @Component
    public static class FileContentL1Cache {
        @Cacheable(value = "FILE_CONTENT_L1", key = "#key")
        public String getFromL1Cache(String key) { return null; }

        @CachePut(value = "FILE_CONTENT_L1", key = "#key")
        public String cacheToL1(String key, String content) { return content; }

        @CacheEvict(value = "FILE_CONTENT_L1", allEntries = true)
        public void evictL1Cache() { }
    }

    // ===== 기존: 경로 기준 원본 코드 조회 (/content/file/raw) =====
    public String getFileContentWithCache(String repoUrl, String filePath, LocalDateTime lastCommit, String authHeader) {
        String cacheKey = "file-content:" + repoUrl + ":" + filePath + ":" + (lastCommit == null ? "" : lastCommit);

        // L1
        String content = l1Cache.getFromL1Cache(cacheKey);
        if (content != null) {
            log.debug("파일 내용 L1 캐시 히트 - key: {}", cacheKey);
            return content;
        }

        // L2
        try {
            content = (String) redisTemplate.opsForValue().get(cacheKey);
            if (content != null) {
                log.debug("파일 내용 L2 캐시 히트 - key: {}", cacheKey);
                l1Cache.cacheToL1(cacheKey, content);
                return content;
            }
        } catch (Exception e) {
            log.warn("Redis 조회 실패, API 직접 호출 - key: {}", cacheKey, e);
        }

        // FastAPI 호출 (원본)
        content = fastApiClient.getFileRaw(repoUrl, filePath, authHeader);

        // 캐싱
        if (content != null) {
            l1Cache.cacheToL1(cacheKey, content);
            try {
                redisTemplate.opsForValue().set(cacheKey, content, Duration.ofHours(2));
            } catch (Exception e) {
                log.warn("Redis 저장 실패 - key: {}", cacheKey, e);
            }
        }
        return content;
    }

    // ===== 추가: 노드 기준 코드 조회 (/content/file/by-node) - AI → 원본(자동 폴백은 FastAPI가 처리) =====
    // FileContentCache.java 안에 넣어 교체
    public String getFileContentByNodeWithCache(
            String repoUrl,
            String nodeKey,
            String filePath,
            String prefer,                 // "auto" | "ai" | "original"
            LocalDateTime lastCommit,
            String authorizationHeader
    ) {
        // 0) prefer 정규화
        String pref = (prefer == null || prefer.isBlank()) ? "auto" : prefer.toLowerCase();
        if (!pref.equals("auto") && !pref.equals("ai") && !pref.equals("original")) {
            pref = "auto";
        }

        // 1) 캐시 키 (레포/커밋/노드/파일/전략 모두 포함)
        String cacheKey = String.format(
                "byNode:%s|%s|%s|%s|%s",
                repoUrl, nodeKey, filePath, pref, String.valueOf(lastCommit)
        );

        // 2) L1 캐시
        String content = l1Cache.getFromL1Cache(cacheKey);
        if (content != null) {
            log.debug("by-node L1 cache hit: {}", cacheKey);
            return content;
        }

        // 3) L2 캐시 (Redis)
        try {
            Object v = redisTemplate.opsForValue().get(cacheKey);
            if (v instanceof String s) {
                log.debug("by-node L2 cache hit: {}", cacheKey);
                l1Cache.cacheToL1(cacheKey, s);
                return s;
            }
        } catch (Exception e) {
            log.warn("Redis get failed (by-node): {}", cacheKey, e);
        }

        // 4) FastAPI 호출 (prefer에 따라 AI/원본 결정은 FastAPI가 먼저 시도)
        String code = null;
        try {
            code = fastApiClient.getCodeByNode(nodeKey, filePath, pref, authorizationHeader);
        } catch (Exception e) {
            log.debug("fastApiClient.getCodeByNode error (nodeKey={}, filePath={}, prefer={}): {}",
                    nodeKey, filePath, pref, e.getMessage());
        }

        // 5) prefer=auto 인 경우에만 원본 폴백 (AI 없을 때)
        if ((code == null || code.isBlank()) && "auto".equals(pref)) {
            try {
                code = fastApiClient.getFileRaw(repoUrl, filePath, authorizationHeader);
            } catch (Exception e) {
                log.debug("fastApiClient.getFileRaw fallback error ({}): {}", filePath, e.getMessage());
            }
        }

        // 6) 캐시에 저장
        if (code != null) {
            l1Cache.cacheToL1(cacheKey, code);
            try {
                redisTemplate.opsForValue().set(cacheKey, code, java.time.Duration.ofHours(2));
            } catch (Exception e) {
                log.warn("Redis set failed (by-node): {}", cacheKey, e);
            }
        }

        // 없으면 null (호출부에서 ""로 대체)
        return code;
    }

    // ===== 캐시 무효화 =====
    public void evictFileCacheForRepo(String repoUrl) {
        // L1 전체 삭제
        l1Cache.evictL1Cache();

        // L2 Redis: repoUrl 기준 삭제
        String pattern = "file-content:" + repoUrl + ":*";
        try {
            Set<String> keysToDelete = redisTemplate.execute((RedisCallback<Set<String>>) connection -> {
                Set<String> keys = new HashSet<>();
                try (Cursor<byte[]> cursor = connection.keyCommands()
                        .scan(ScanOptions.scanOptions().match(pattern).count(200).build())) {
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
