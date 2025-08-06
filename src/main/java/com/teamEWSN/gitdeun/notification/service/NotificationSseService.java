package com.teamEWSN.gitdeun.notification.service;

import com.teamEWSN.gitdeun.notification.dto.UnreadNotificationCountDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class NotificationSseService {

    // 스레드 안전한 자료구조를 사용하여 사용자별 Emitter를 관리 (Key: userId, Value: SseEmitter)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 클라이언트가 구독을 요청할 때 호출
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃을 매우 길게 설정
        emitters.put(userId, emitter);

        // 연결 종료 시 Emitter 제거
        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));


        // 연결 성공을 알리는 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("Connected to notification stream."));
        } catch (IOException e) {
            log.error("SSE 연결 중 오류 발생, userId={}", userId, e);
        }

        return emitter;
    }

    // 특정 사용자에게 읽지 않은 알림 개수 전송
    public void sendUnreadCount(Long userId, int count) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name("unread-count") // 이벤트 이름 지정
                    .data(new UnreadNotificationCountDto(count))); // 데이터 전송
            } catch (IOException e) {
                log.error("SSE 데이터 전송 중 오류 발생, userId={}", userId, e);
                // 오류 발생 시 해당 Emitter 제거
                emitters.remove(userId);
            }
        }
    }
}