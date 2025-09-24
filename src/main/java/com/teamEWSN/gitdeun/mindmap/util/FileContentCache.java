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
    public String getFileContentByNodeWithCache(
            String nodeKey,
            String filePath,
            String prefer,                 // "auto" | "ai" | "original"
            String authorizationHeader,
            LocalDateTime lastCommit
    ) {
        String cacheKey = "byNode:" + nodeKey + "|" + filePath + "|" + (prefer == null ? "auto" : prefer)
                + "|" + (lastCommit == null ? "" : lastCommit);

        // L1
        String cached = l1Cache.getFromL1Cache(cacheKey);
        if (cached != null) {
            log.debug("by-node L1 히트 - {}", cacheKey);
            return cached;
        }

        // L2
        try {
            String l2 = (String) redisTemplate.opsForValue().get(cacheKey);
            if (l2 != null) {
                log.debug("by-node L2 히트 - {}", cacheKey);
                l1Cache.cacheToL1(cacheKey, l2);
                return l2;
            }
        } catch (Exception e) {
            log.warn("by-node Redis 조회 실패 - {}", cacheKey, e);
        }

        // FastAPI: /content/file/by-node?node_key=...&file_path=...&prefer=...
        String url = UriComponentsBuilder
                .fromHttpUrl(fastApiBaseUrl + "/content/file/by-node")
                .queryParam("node_key", nodeKey)
                .queryParam("file_path", filePath)
                .queryParam("prefer", prefer == null ? "auto" : prefer)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        if (authorizationHeader != null && !authorizationHeader.isBlank()) {
            headers.set("Authorization", authorizationHeader);
        }
        HttpEntity<Void> req = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> resp = restTemplate.exchange(url, HttpMethod.GET, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful() && resp.getBody() != null) {
                Object code = resp.getBody().get("code");
                if (code instanceof String s && !s.isEmpty()) {
                    // 캐싱
                    l1Cache.cacheToL1(cacheKey, s);
                    try {
                        redisTemplate.opsForValue().set(cacheKey, s, Duration.ofHours(2));
                    } catch (Exception e) {
                        log.warn("by-node Redis 저장 실패 - {}", cacheKey, e);
                    }
                    return s;
                }
            }
        } catch (HttpStatusCodeException e) {
            log.debug("by-node 호출 실패: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("by-node 호출 에러", e);
        }
        return "";
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
