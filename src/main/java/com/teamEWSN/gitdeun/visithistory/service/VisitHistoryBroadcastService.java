package com.teamEWSN.gitdeun.visithistory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.visithistory.dto.PinnedHistoryUpdateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 방문 기록 핀 고정/해제에 대한 실시간 알림 서비스
 * - 사용자별 SSE 연결 관리
 * - 핀 상태 변경 시 실시간 브로드캐스트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitHistoryBroadcastService {

    private final ObjectMapper objectMapper;

    // 사용자별 SSE 연결 관리 (한 사용자당 여러 탭 가능)
    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> userConnections = new ConcurrentHashMap<>();

    private static final long TIMEOUT_MS = 30L * 60L * 1000L; // 30분

    /**
     * 사용자의 방문 기록 페이지 SSE 연결 생성
     */
    public SseEmitter createVisitHistoryConnection(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 사용자별 연결 목록에 추가
        userConnections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료 시 정리
        emitter.onCompletion(() -> removeConnection(userId, emitter));
        emitter.onTimeout(() -> removeConnection(userId, emitter));
        emitter.onError(throwable -> {
            log.error("방문기록 SSE 연결 오류 - 사용자 ID: {}", userId, throwable);
            removeConnection(userId, emitter);
        });

        // 연결 확인 메시지
        sendToEmitter(emitter);

        log.info("방문기록 SSE 연결 생성 - 사용자 ID: {}", userId);
        return emitter;
    }

    /**
     * 핀 고정/해제 상태 변경 브로드캐스트
     */
    public void broadcastPinUpdate(Long userId, PinnedHistoryUpdateDto updateDto) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);

        if (connections == null || connections.isEmpty()) {
            log.debug("사용자 ID {} 의 활성 연결이 없음", userId);
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(updateDto);

            // 실패한 연결들 수집
            CopyOnWriteArrayList<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            connections.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                        .name("pin_update")
                        .data(jsonData));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                    log.warn("핀 업데이트 SSE 전송 실패 - 사용자 ID: {}", userId, e);
                }
            });

            // 실패한 연결 제거
            deadEmitters.forEach(emitter -> removeConnection(userId, emitter));

            log.info("핀 상태 변경 브로드캐스트 완료 - 사용자 ID: {}, 액션: {}, 성공: {}, 실패: {}",
                userId, updateDto.getAction(),
                connections.size() - deadEmitters.size(), deadEmitters.size());

        } catch (Exception e) {
            log.error("핀 업데이트 브로드캐스트 실패 - 사용자 ID: {}", userId, e);
        }
    }

    /**
     * 개별 emitter에게 메시지 전송
     */
    private void sendToEmitter(SseEmitter emitter) {
        try {
            String jsonData = objectMapper.writeValueAsString("방문기록 실시간 연결 성공");
            emitter.send(SseEmitter.event()
                .name("connected")
                .data(jsonData));
        } catch (IOException e) {
            log.warn("초기 SSE 메시지 전송 실패", e);
        }
    }

    /**
     * 연결 제거
     */
    private void removeConnection(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> connections = userConnections.get(userId);
        if (connections != null) {
            connections.remove(emitter);
            if (connections.isEmpty()) {
                userConnections.remove(userId);
                log.info("사용자 ID {} 의 모든 방문기록 SSE 연결 종료", userId);
            }
        }
    }

}