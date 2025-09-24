package com.teamEWSN.gitdeun.mindmap.util;

import com.teamEWSN.gitdeun.common.fastapi.FastApiClient;
import com.teamEWSN.gitdeun.common.fastapi.dto.MindmapGraphDto;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MindmapGraphCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final FastApiClient fastApiClient;
    private final MindmapL1Cache mindmapL1Cache;

    // L2 캐시: Redis
    public MindmapGraphResponseDto getGraphWithHybridCache(String repoUrl, LocalDateTime lastCommit, String authHeader) {
        String mapId = extractMapId(repoUrl);
        String versionedMapId = mapId + ":" + lastCommit.toString(); // 버전 정보를 포함한 키

        // 1. L1 캐시 확인 (Caffeine)
        MindmapGraphResponseDto l1Result = mindmapL1Cache.getGraphFromL1Cache(versionedMapId);
        if (l1Result != null) {
            log.debug("마인드맵 그래프 L1 캐시 히트 - key: {}", versionedMapId);
            return l1Result;
        }

        // 2. L2 캐시 확인 (Redis)
        String redisKey = "mindmap:graph:" + versionedMapId;
        try {
            MindmapGraphResponseDto l2Result = (MindmapGraphResponseDto) redisTemplate.opsForValue().get(redisKey);

            if (l2Result != null) {
                log.debug("마인드맵 그래프 L2 캐시 히트 - key: {}", redisKey);
                mindmapL1Cache.cacheToL1(versionedMapId, l2Result);
                return l2Result;
            }
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패 - key: {}, FastAPI 직접 호출로 진행", redisKey, e);
        }

        // 3. FastAPI 실시간 조회
        try {
            log.debug("FastAPI 실시간 조회 시작 - mapId: {}", mapId);
            MindmapGraphDto graphDto = fastApiClient.getGraph(mapId, authHeader);
            MindmapGraphResponseDto responseDto = convertToResponseDto(graphDto);

            // 4. 양방향 캐싱 (버전 포함된 키 사용)
            try {
                redisTemplate.opsForValue().set(redisKey, responseDto, Duration.ofHours(2)); // L2: 2시간
                log.debug("Redis 캐시 저장 완료 - key: {}", redisKey);
            } catch (Exception e) {
                log.warn("Redis 캐시 저장 실패 - key: {}", redisKey, e);
            }

            mindmapL1Cache.cacheToL1(versionedMapId, responseDto);

            return responseDto;

        } catch (Exception e) {
            log.error("FastAPI 그래프 조회 실패 - mapId: {}", mapId, e);
            return MindmapGraphResponseDto.builder()
                .success(false)
                .error("그래프 데이터 조회 실패: " + e.getMessage())
                .graphMapId(mapId)
                .nodeCount(0)
                .build();
        }
    }

    // 캐시 무효화 (마인드맵 새로고침 또는 프롬프트 적용 시)
    public void evictCache(String repoUrl, LocalDateTime lastCommit) {
        String mapId = extractMapId(repoUrl);
        String versionedMapId = mapId + ":" + lastCommit.toString();
        String redisKey = "mindmap:graph:" + versionedMapId;

        try {
            redisTemplate.delete(redisKey);
            mindmapL1Cache.evictL1Cache(versionedMapId);
            log.info("마인드맵 그래프 캐시 무효화 완료 - key: {}", redisKey);
        } catch (Exception e) {
            log.warn("캐시 무효화 실패 - key: {}", redisKey, e);
        }
    }

    // === Helper Methods ===
    private String extractMapId(String repoUrl) {
        String[] segments = repoUrl.split("/");
        return segments[segments.length - 1].replaceAll("\\.git$", "");
    }

    private MindmapGraphResponseDto convertToResponseDto(MindmapGraphDto graphDto) {
        return MindmapGraphResponseDto.builder()
            .success(true)
            .error(null)
            .graphMapId(graphDto.getMapId())
            .nodeCount(graphDto.getCount())
            .nodes(graphDto.getNodes())
            .edges(graphDto.getEdges())
            .build();
    }
}