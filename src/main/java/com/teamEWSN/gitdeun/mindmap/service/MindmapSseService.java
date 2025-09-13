package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapSseService {

    private final ObjectMapper objectMapper;

    private final Map<String, SseEmitter> userConnections = new ConcurrentHashMap<>();
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 타임아웃 설정(1시간)
    private static final long TIMEOUT_MS = 60L * 60L * 1000L;

    /**
     * 마인드맵 실시간 연결 생성
     */
    public SseEmitter createConnection(Long mapId, Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        emitters.computeIfAbsent(mapId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 사용자별 연결 추가 추적
        String userKey = mapId + "_" + userId;
        userConnections.put(userKey, emitter);

        // 연결 종료 시 정리 (기존 로직 개선)
        emitter.onCompletion(() -> removeEmitter(mapId, userId, emitter));
        emitter.onTimeout(() -> removeEmitter(mapId, userId, emitter));
        emitter.onError(throwable -> {
            log.error("SSE 연결 오류 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userId, throwable);
            removeEmitter(mapId, userId, emitter);
        });

        // 연결 확인용 초기 메시지
        sendToEmitter(emitter, "마인드맵 " + mapId + " 실시간 연결 성공");

        log.info("SSE 연결 생성 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userId);
        return emitter;
    }

    /**
     * 마인드맵 업데이트 브로드캐스트
     */
    public void broadcastUpdate(Long mapId, MindmapDetailResponseDto data) {
        sendToMapSubscribers(mapId, "mindmap-update", data);
    }

    /**
     * 프롬프트 적용 브로드캐스트
     */
    public void broadcastPromptApplied(Long mapId, Long historyId) {
        Map<String, Object> eventData = Map.of(
            "type", "prompt_applied",
            "historyId", historyId,
            "message", "새로운 프롬프트가 적용되었습니다."
        );
        sendToMapSubscribers(mapId, "prompt-applied", eventData);
    }

    /**
     * 제목 변경 브로드캐스트
     */
    public void broadcastTitleChanged(Long mapId, String newTitle) {
        Map<String, Object> eventData = Map.of(
            "type", "title_changed",
            "newTitle", newTitle,
            "message", "마인드맵 제목이 변경되었습니다."
        );
        sendToMapSubscribers(mapId, "title-changed", eventData);
    }

    /**
     * 특정 마인드맵의 모든 구독자에게 이벤트 전송
     */
    private void sendToMapSubscribers(Long mapId, String eventName, Object data) {
        List<SseEmitter> mapEmitters = emitters.get(mapId);
        if (mapEmitters == null || mapEmitters.isEmpty()) {
            log.debug("마인드맵 ID {} 에 연결된 클라이언트가 없음", mapId);
            return;
        }

        // JSON 직렬화 한 번만 수행
        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("SSE 데이터 직렬화 실패 - 마인드맵 ID: {}", mapId, e);
            return;
        }

        // 전송 실패한 emitter 추적
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        mapEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(jsonData));
            } catch (IOException e) {
                deadEmitters.add(emitter);
                log.warn("SSE 전송 실패 - 마인드맵 ID: {}, 이벤트: {}", mapId, eventName, e);
            }
        });

        // 실패한 emitter 정리
        deadEmitters.forEach(emitter -> removeEmitterOnly(mapId, emitter));

        log.debug("SSE 브로드캐스트 완료 - 마인드맵 ID: {}, 이벤트: {}, 성공: {}, 실패: {}",
            mapId, eventName, mapEmitters.size() - deadEmitters.size(), deadEmitters.size());
    }

    /**
     * 개별 emitter에게 초기 메시지 전송
     */
    private void sendToEmitter(SseEmitter emitter, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(jsonData));
        } catch (IOException e) {
            log.warn("초기 SSE 메시지 전송 실패", e);
            // 초기 연결 실패는 런타임 예외로 처리하지 않음
        }
    }

    /**
     * 사용자별 emitter 제거
     */
    private void removeEmitter(Long mapId, Long userId, SseEmitter emitter) {
        // 사용자별 연결 제거
        String userKey = mapId + "_" + userId;
        userConnections.remove(userKey);

        // 마인드맵별 연결 제거
        removeEmitterOnly(mapId, emitter);

        log.info("SSE 연결 해제 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userId);
    }

    /**
     * emitter만 제거
     */
    private void removeEmitterOnly(Long mapId, SseEmitter emitter) {
        List<SseEmitter> mapEmitters = emitters.get(mapId);
        if (mapEmitters != null) {
            mapEmitters.remove(emitter);
            if (mapEmitters.isEmpty()) {
                emitters.remove(mapId);
                log.info("마인드맵 ID {} 의 모든 구독자 연결 종료", mapId);
            }
        }
    }

    /**
     * 연결 수 조회
     */
    public int getConnectionCount(Long mapId) {
        List<SseEmitter> mapEmitters = emitters.get(mapId);
        return mapEmitters != null ? mapEmitters.size() : 0;
    }
}