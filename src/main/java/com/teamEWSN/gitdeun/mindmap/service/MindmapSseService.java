package com.teamEWSN.gitdeun.mindmap.service;

import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MindmapSseService {

    // 스레드 안전(thread-safe)한 자료구조 사용
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    // 1시간 타임아웃 설정
    private static final long TIMEOUT_MS = 60L * 60L * 1000L;

    /**
     * 클라이언트가 마인드맵 업데이트 구독을 요청할 때 호출됩니다.
     */
    public SseEmitter subscribe(Long mapId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 스레드 안전하게 emitter 추가
        emitters.computeIfAbsent(mapId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료, 타임아웃, 오류 발생 시 emitter 제거
        emitter.onCompletion(() -> removeEmitter(mapId, emitter));
        emitter.onTimeout(() -> removeEmitter(mapId, emitter));
        emitter.onError(e -> removeEmitter(mapId, emitter));

        // 연결 성공을 알리는 초기 이벤트 전송
        sendToEmitter(emitter, "connect", "Connected to mindmap updates for mapId: " + mapId);

        return emitter;
    }

    /**
     * 마인드맵 업데이트 시 모든 구독자에게 브로드캐스트합니다.
     */
    public void broadcastUpdate(Long mapId, MindmapDetailResponseDto updatedMindmap) {
        sendToMapSubscribers(mapId, "mindmap-update", updatedMindmap);
    }

    /**
     * 특정 마인드맵의 모든 구독자에게 이벤트를 전송합니다.
     */
    private void sendToMapSubscribers(Long mapId, String eventName, Object data) {
        List<SseEmitter> mapEmitters = emitters.getOrDefault(mapId, Collections.emptyList());
        if (mapEmitters.isEmpty()) {
            return;
        }

        // 전송 실패한 emitter를 추적하기 위한 리스트
        List<SseEmitter> deadEmitters = new ArrayList<>();
        mapEmitters.forEach(emitter -> {
            try {
                sendToEmitter(emitter, eventName, data);
            } catch (Exception e) {
                deadEmitters.add(emitter);
                log.warn("Failed to send SSE to mapId={}: {}", mapId, e.toString());
            }
        });

        // 실패한 emitter 정리
        deadEmitters.forEach(emitter -> removeEmitter(mapId, emitter));
    }

    /**
     * 개별 emitter에게 이벤트를 전송합니다.
     */
    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            // 전송 실패 시 런타임 예외 발생
            throw new RuntimeException("SSE data sending failed", e);
        }
    }

    /**
     * 특정 마인드맵 구독 목록에서 emitter를 제거합니다.
     * 리스트가 비면 맵에서도 해당 항목을 삭제합니다.
     */
    private void removeEmitter(Long mapId, SseEmitter emitter) {
        List<SseEmitter> mapEmitters = emitters.get(mapId);
        if (mapEmitters != null) {
            mapEmitters.remove(emitter);
            if (mapEmitters.isEmpty()) {
                emitters.remove(mapId);
                log.info("No more subscribers for mapId={}, removing from map.", mapId);
            }
        }
    }
}
