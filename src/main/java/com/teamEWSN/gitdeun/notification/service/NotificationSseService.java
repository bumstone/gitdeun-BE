package com.teamEWSN.gitdeun.notification.service;

import com.teamEWSN.gitdeun.notification.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSseService {

    // 한 사용자에 대해 여러 탭/기기의 연결을 허용
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    // 타임아웃 설정
    private static final long TIMEOUT_MS = 60L * 60L * 1000L; // 1시간


    /** 클라이언트 구독 */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 사용자별 리스트에 emitter 추가 (동시성 안전)
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        // 연결 종료/에러 시 정리
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        // 헬스체크/연결 확인
        sendToEmitter(emitter, "connect", "connected");
        return emitter;
    }

    /** 읽지 않은 알림 개수 전송 */
    public void sendUnreadCount(Long userId, int count) {
        sendToUser(userId, "unreadCount", count);
    }

    /** 새 알림 전송 */
    public void sendNewNotification(Long userId, NotificationResponseDto notification) {
        sendToUser(userId, "newNotification", notification);
    }

    /** 사용자에게 이벤트 전송 */
    private void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.getOrDefault(userId, Collections.emptyList());
        if (userEmitters.isEmpty()) return;

        List<SseEmitter> dead = new ArrayList<>();
        for (SseEmitter emitter : userEmitters) {
            try {
                sendToEmitter(emitter, eventName, data);
            } catch (Exception e) {
                dead.add(emitter);
                log.warn("Failed to send SSE (userId={}): {}", userId, e.toString());
            }
        }
        // 죽은 emitter 정리
        dead.forEach(em -> removeEmitter(userId, em));
    }

    /** 개별 emitter에게 전송 */
    private void sendToEmitter(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            throw new RuntimeException("SSE 전송 실패", e);
        }
    }

    /** emitter 제거(리스트가 비면 맵에서도 제거) */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) emitters.remove(userId);
    }
}
