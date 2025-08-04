package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MindmapSseService {

    // 스레드 안전(thread-safe)한 자료구조 사용
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 클라이언트가 구독을 요청할 때 호출
    public SseEmitter subscribe(Long mapId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃을 매우 길게 설정
        // 특정 마인드맵 ID에 해당하는 Emitter 리스트에 추가
        this.emitters.computeIfAbsent(mapId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> this.emitters.get(mapId).remove(emitter));
        emitter.onTimeout(() -> this.emitters.get(mapId).remove(emitter));

        // 연결 성공을 알리는 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected!"));
        } catch (IOException e) {
            log.error("SSE 연결 중 오류 발생", e);
        }

        return emitter;
    }

    // 마인드맵이 업데이트되면 이 메서드를 호출하여 모든 구독자에게 방송
    public void broadcastUpdate(Long mapId, MindmapDetailResponseDto updatedMindmap) {
        List<SseEmitter> mapEmitters = this.emitters.get(mapId);
        if (mapEmitters == null) {
            return;
        }

        mapEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("mindmap-update") // 이벤트 이름 지정
                    .data(updatedMindmap)); // 업데이트된 마인드맵 데이터 전송
            } catch (IOException e) {
                log.error("SSE 데이터 전송 중 오류 발생, emitter 제거", e);
                mapEmitters.remove(emitter);
            }
        });
    }
}
