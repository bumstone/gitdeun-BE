package com.teamEWSN.gitdeun.mindmap.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import com.teamEWSN.gitdeun.mindmap.dto.MindmapDetailResponseDto;
import com.teamEWSN.gitdeun.mindmap.dto.prompt.PromptPreviewResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MindmapSseService {

    private final ObjectMapper objectMapper;

    private record SseConnection(Long userId, String nickname, String profileImage, SseEmitter emitter) {}
    private final Map<Long, List<SseConnection>> connectionsByMapId = new ConcurrentHashMap<>();

    // 타임아웃 설정(1시간)
    private static final long TIMEOUT_MS = 60L * 60L * 1000L;

    /**
     * 마인드맵 실시간 연결 생성
     */
    public SseEmitter createConnection(Long mapId, CustomUserDetails userDetails) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);

        // 사용자 정보와 Emitter를 함께 SseConnection 객체로 감싸서 저장
        var connection = new SseConnection(
            userDetails.getId(),
            userDetails.getNickname(),
            userDetails.getProfileImage(),
            emitter
        );

        connectionsByMapId.computeIfAbsent(mapId, k -> new CopyOnWriteArrayList<>()).add(connection);


        // 연결 종료 시 정리 (기존 로직 개선)
        emitter.onCompletion(() -> removeConnection(mapId, connection));
        emitter.onTimeout(() -> removeConnection(mapId, connection));
        emitter.onError(throwable -> {
            log.error("SSE 연결 오류 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId(), throwable);
            removeConnection(mapId, connection);
        });

        // 연결 확인용 초기 메시지
        sendToEmitter(emitter, "마인드맵 " + mapId + " 실시간 연결 성공");

        log.info("SSE 연결 생성 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, userDetails.getId());
        return emitter;
    }

    /**
     * 마인드맵 업데이트 브로드캐스트
     */
    public void broadcastUpdate(Long mapId, MindmapDetailResponseDto data) {
        sendToMapSubscribers(mapId, "mindmap-update", data);
    }

    /**
     * 새로운 프롬프트 미리보기 준비 완료 브로드캐스트
     */
    public void broadcastPromptReady(Long mapId, PromptPreviewResponseDto data) {
        sendToMapSubscribers(mapId, "prompt-ready", data);
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
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        if (connections == null || connections.isEmpty()) {
            log.debug("마인드맵 ID {} 에 연결된 클라이언트가 없음", mapId);
            return;
        }

        String jsonData;
        try {
            jsonData = objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("SSE 데이터 직렬화 실패 - 마인드맵 ID: {}", mapId, e);
            return;
        }

        List<SseConnection> deadConnections = new CopyOnWriteArrayList<>();

        connections.forEach(connection -> {
            try {
                connection.emitter().send(SseEmitter.event().name(eventName).data(jsonData));
            } catch (IOException e) {
                deadConnections.add(connection);
                log.warn("SSE 전송 실패 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, connection.userId(), e);
            }
        });

        // 실패한 연결 정리
        deadConnections.forEach(connection -> removeConnection(mapId, connection));

        log.debug("SSE 브로드캐스트 완료 - 마인드맵 ID: {}, 이벤트: {}, 성공: {}, 실패: {}",
            mapId, eventName, connections.size() - deadConnections.size(), deadConnections.size());
    }

    /**
     * 개별 emitter에게 초기 메시지 전송
     */
    private void sendToEmitter(SseEmitter emitter, Object data) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            emitter.send(SseEmitter.event().name("connected").data(jsonData));
        } catch (IOException e) {
            log.warn("초기 SSE 메시지 전송 실패", e);
        }
    }

    // 연결 종료 시 SseConnection 객체를 찾아 제거
    private void removeConnection(Long mapId, SseConnection connection) {
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        if (connections != null) {
            connections.remove(connection);
            log.info("SSE 연결 해제 - 마인드맵 ID: {}, 사용자 ID: {}", mapId, connection.userId());
            if (connections.isEmpty()) {
                connectionsByMapId.remove(mapId);
                log.info("마인드맵 ID {} 의 모든 구독자 연결 종료", mapId);
            }
        }
    }


    // 접속된 사용자 수 조회
    public int getConnectionCount(Long mapId) {
        List<SseConnection> connections = connectionsByMapId.get(mapId);
        return connections != null ? connections.size() : 0;
    }

    // 접속자 정보 DTO
    public record ConnectedUserDto(Long userId, String nickname, String profileImage) {}

    // 현재 접속 중인 사용자 목록을 반환하는 서비스 메서드
    public List<ConnectedUserDto> getConnectedUsers(Long mapId) {
        List<SseConnection> connections = connectionsByMapId.getOrDefault(mapId, new CopyOnWriteArrayList<>());

        // 중복된 userId를 제거하고 DTO로 변환하여 반환 (여러 탭 접속 시 중복 방지)
        return connections.stream()
            .map(conn -> new ConnectedUserDto(conn.userId(), conn.nickname(), conn.profileImage()))
            .distinct() // userId 기준으로 중복 제거
            .collect(Collectors.toList());
    }

}