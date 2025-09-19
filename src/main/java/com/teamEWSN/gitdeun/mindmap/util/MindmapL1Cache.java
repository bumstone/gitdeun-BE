package com.teamEWSN.gitdeun.mindmap.util;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapGraphResponseDto;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class MindmapL1Cache {

  // L1 캐시에서 데이터를 가져오는 역할만 수행
  // 캐시가 있으면 반환, 없으면 null 반환
  @Cacheable(value = "MINDMAP_GRAPH_L1", key = "#mapId")
  public MindmapGraphResponseDto getGraphFromL1Cache(String mapId) {
    return null;
  }

  // L1 캐시에 데이터를 저장하는 역할만 수행
  @CachePut(value = "MINDMAP_GRAPH_L1", key = "#mapId")
  public MindmapGraphResponseDto cacheToL1(String mapId, MindmapGraphResponseDto data) {
    return data;
  }

  // L1 캐시 데이터 삭제
  @CacheEvict(value = "MINDMAP_GRAPH_L1", key = "#mapId")
  public void evictL1Cache(String mapId) {}
}